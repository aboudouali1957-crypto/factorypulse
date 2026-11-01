package com.factorypulse.alert.it;

import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
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
import org.springframework.http.MediaType;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TEST D'INTÉGRATION : l'alert-service complet, avec un VRAI Kafka et un VRAI MongoDB (Testcontainers).
 *
 * Vérifie le parcours de bout en bout (événement Kafka → alerte en base → API) et surtout
 * les règles d'accès par rôle : 401 sans jeton, 403 avec le mauvais rôle.
 */
@SpringBootTest(properties = "spring.kafka.consumer.auto-offset-reset=earliest")
@AutoConfigureMockMvc
@Testcontainers
@Import(AlertServiceIT.TopicConfig.class)
class AlertServiceIT {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", () -> MONGO.getReplicaSetUrl("factorypulse_alerts_it"));
    }

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
    private AlertRepository repository;

    @Autowired
    private MockMvc mockMvc;

    /** Jeton simulé d'un utilisateur : son nom (sub) et son rôle. */
    private static JwtRequestPostProcessor user(String username, String role) {
        return jwt().jwt(token -> token.subject(username)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static String panne(String machine, long timestamp) {
        return """
                {"eventId":"d%d","machineId":"%s","type":"MACHINE_DOWN","timestamp":%d,"good":null,"energyKwh":null}
                """.formatted(timestamp, machine, timestamp);
    }

    private Alert declencherUnePanne(String machine) throws InterruptedException {
        producer().send("machine-events", machine, panne(machine, System.currentTimeMillis()));
        producer().flush();
        return waitUntil(() -> repository.findActiveByMachine(machine), Duration.ofSeconds(60)).get(0);
    }

    @Test
    void unePanneDansKafka_devientUneAlerte_visibleDansLApi() throws Exception {
        Alert alert = declencherUnePanne("M1");

        mockMvc.perform(get("/api/alerts/active").with(user("operateur", "OPERATEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '%s')].machineId", alert.id()).value("M1"));
    }

    @Test
    void sansJeton_401() throws Exception {
        mockMvc.perform(get("/api/alerts")).andExpect(status().isUnauthorized());
    }

    @Test
    void priseEnCharge_reserveeALOperateur() throws Exception {
        Alert alert = declencherUnePanne("M2");
        String url = "/api/alerts/" + alert.id() + "/acknowledge";

        mockMvc.perform(post(url).with(user("responsable", "RESPONSABLE")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(url).with(user("operateur", "OPERATEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgedBy").value("operateur"));

        // Une deuxième prise en charge est refusée : 409 avec un message clair.
        mockMvc.perform(post(url).with(user("operateur", "OPERATEUR")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());

        assertThat(repository.findById(alert.id()).orElseThrow().acknowledgedBy()).isEqualTo("operateur");
    }

    @Test
    void interventions_saisieParLOperateur_historiqueReserveAuResponsable() throws Exception {
        String body = """
                {"machineId":"M3","alertId":null,"cause":"MECANIQUE","description":"Courroie remplacée","durationMinutes":20}
                """;

        mockMvc.perform(post("/api/interventions").with(user("responsable", "RESPONSABLE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/interventions").with(user("operateur", "OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.performedBy").value("operateur"));

        mockMvc.perform(get("/api/interventions").with(user("operateur", "OPERATEUR")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/interventions").with(user("responsable", "RESPONSABLE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Courroie remplacée"));
    }

    @Test
    void uneSaisieInvalide_400AvecLeMessage() throws Exception {
        String body = """
                {"machineId":"M1","cause":"REGLAGE","description":"Réglage","durationMinutes":0}
                """;
        mockMvc.perform(post("/api/interventions").with(user("operateur", "OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").exists());
    }

    /** Réessaie toutes les 200 ms jusqu'à obtenir une liste non vide (le traitement Kafka est asynchrone). */
    private static <T> List<T> waitUntil(Supplier<List<T>> check, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            List<T> value = check.get();
            if (!value.isEmpty()) {
                return value;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Condition non atteinte après " + timeout.toSeconds() + " s");
    }
}
