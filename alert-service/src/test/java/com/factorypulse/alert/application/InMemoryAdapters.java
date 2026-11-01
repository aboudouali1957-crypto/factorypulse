package com.factorypulse.alert.application;

import com.factorypulse.alert.application.port.out.AlertPublisher;
import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.application.port.out.InterventionRepository;
import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.Intervention;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Faux adaptateurs en mémoire, partagés par les tests des cas d'usage (à la place de MongoDB, Kafka, WebSocket). */
final class InMemoryAdapters {

    private InMemoryAdapters() {
    }

    static class AlertStore implements AlertRepository {
        final Map<String, Alert> store = new LinkedHashMap<>();

        @Override
        public void save(Alert alert) {
            store.put(alert.id(), alert);
        }

        @Override
        public Optional<Alert> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Alert> findActive() {
            return store.values().stream().filter(Alert::isActive).toList();
        }

        @Override
        public List<Alert> findActiveByMachine(String machineId) {
            return findActive().stream().filter(a -> a.machineId().equals(machineId)).toList();
        }

        @Override
        public List<Alert> findRecent(int limit) {
            return store.values().stream()
                    .sorted(Comparator.comparingLong(Alert::updatedAt).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    static class Publisher implements AlertPublisher {
        final List<Alert> published = new ArrayList<>();

        @Override
        public void publish(Alert alert) {
            published.add(alert);
        }
    }

    static class InterventionStore implements InterventionRepository {
        final List<Intervention> store = new ArrayList<>();

        @Override
        public void save(Intervention intervention) {
            store.add(intervention);
        }

        @Override
        public List<Intervention> findRecent(int limit) {
            return store.stream()
                    .sorted(Comparator.comparingLong(Intervention::createdAt).reversed())
                    .limit(limit)
                    .toList();
        }
    }
}
