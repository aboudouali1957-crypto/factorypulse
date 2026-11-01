package com.factorypulse.alert.adapter.out.mongo;

import com.factorypulse.alert.application.port.out.AlertRepository;
import com.factorypulse.alert.domain.Alert;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ADAPTATEUR DE SORTIE : implémente le port {@link AlertRepository} avec MongoDB.
 */
@Component
public class MongoAlertRepository implements AlertRepository {

    private final SpringDataAlertRepository springData;

    public MongoAlertRepository(SpringDataAlertRepository springData) {
        this.springData = springData;
    }

    @Override
    public void save(Alert alert) {
        springData.save(AlertDocument.from(alert));
    }

    @Override
    public List<Alert> findActive() {
        return springData.findByResolvedAtIsNullOrderByRaisedAtDesc().stream().map(AlertDocument::toDomain).toList();
    }

    @Override
    public List<Alert> findActiveByMachine(String machineId) {
        return springData.findByMachineIdAndResolvedAtIsNull(machineId).stream().map(AlertDocument::toDomain).toList();
    }

    @Override
    public List<Alert> findRecent(int limit) {
        return springData.findAllByOrderByUpdatedAtDesc(PageRequest.of(0, limit))
                .stream().map(AlertDocument::toDomain).toList();
    }
}
