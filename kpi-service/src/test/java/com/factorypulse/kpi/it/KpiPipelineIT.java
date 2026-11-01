package com.factorypulse.kpi.it;

import com.factorypulse.kpi.application.port.out.MachineKpiRepository;
import com.factorypulse.kpi.domain.MachineKpi;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TEST D'INTÉGRATION : le kpi-service complet, avec un VRAI Kafka et un VRAI MongoDB
 * lancés dans Docker par Testcontainers.
 *
 * On publie des messages JSON dans Kafka (comme le simulateur), puis on vérifie que les indicateurs
 * sont calculés, sauvegardés dans MongoDB et exposés par l'API, protégée par jeton.
 *
 * Les jetons sont simulés par spring-security-test (jwt()) : Keycloak n'est pas nécessaire ici.
 * Lancement : "mvn verify" (Docker doit tourner). "mvn test" ne lance que les tests unitaires.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(KpiPipelineIT.TopicConfig.class)
class KpiPipelineIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("factorypulse_it"));
    }

    /** Crée le topic au démarrage (en production, c'est le simulateur qui le crée). */
    @TestConfiguration
    static class TopicConfig {
        @Bean
        NewTopic machineEventsTopic() {
            return TopicBuilder.name("machine-events").partitions(3).replicas(1).build();
        }
    }

    /**
     * Producteur propre au test, qui envoie du texte brut (comme le JSON du simulateur).
     * Ce n'est volontairement PAS un bean Spring : il ne doit pas remplacer celui de l'application.
     */
    private static KafkaTemplate<String, String> producer;

    /** Créé au premier usage : le conteneur Kafka est alors démarré et son adresse connue. */
    private static KafkaTemplate<String, String> producer() {
        if (producer == null) {
            producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, String>(java.util.Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                    ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                    ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class)));
        }
        return producer;
    }

    @Autowired
    private MachineKpiRepository repository;

    @Autowired
    private MockMvc mockMvc;

    private static final long T0 = 1_767_225_600_000L;

    /** Un message JSON identique à ceux du simulateur. */
    private static String event(String id, String type, long atSecond, Boolean good) {
        return """
                {"eventId":"%s","machineId":"M1","type":"%s","timestamp":%d,"good":%s,"energyKwh":null}
                """.formatted(id, type, T0 + atSecond * 1000, good);
    }

    @Test
    void lesEvenementsKafka_deviennentDesIndicateurs_sauvegardesEtExposes() throws Exception {
        producer().send("machine-events", "M1", event("e1", "PIECE_PRODUCED", 0, true));
        producer().send("machine-events", "M1", event("e2", "PIECE_PRODUCED", 2, true));
        producer().send("machine-events", "M1", event("e3", "PIECE_PRODUCED", 4, false));
        producer().flush();

        MachineKpi kpi = waitUntil(() -> repository.findById("M1")
                .filter(k -> k.snapshot().piecesProduced() == 3), Duration.ofSeconds(60));

        assertThat(kpi.snapshot().rejectedPieces()).isEqualTo(1);

        mockMvc.perform(get("/api/kpis/M1").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.machineId").value("M1"))
                .andExpect(jsonPath("$.piecesProduced").value(3))
                .andExpect(jsonPath("$.recent.windowMinutes").value(15));
    }

    @Test
    void sansJeton_lApiRepond401() throws Exception {
        mockMvc.perform(get("/api/kpis")).andExpect(status().isUnauthorized());
    }

    @Test
    void unMessageIllisible_nebloquePasLaLecture() throws Exception {
        producer().send("machine-events", "M1", "ceci n'est pas du JSON");
        producer().send("machine-events", "M1", event("e10", "PIECE_PRODUCED", 10, true)
                .replace("\"M1\"", "\"M2\""));
        producer().flush();

        // Le message suivant (M2) est quand même traité : l'ErrorHandlingDeserializer a ignoré le mauvais.
        waitUntil(() -> repository.findById("M2"), Duration.ofSeconds(60));
    }

    /** Réessaie toutes les 200 ms jusqu'à obtenir une valeur (le traitement Kafka est asynchrone). */
    private static <T> T waitUntil(java.util.function.Supplier<Optional<T>> check, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            Optional<T> value = check.get();
            if (value.isPresent()) {
                return value.get();
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Condition non atteinte après " + timeout.toSeconds() + " s");
    }
}
