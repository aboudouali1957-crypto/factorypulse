package com.factorypulse.alert.adapter.out.mongo;

import com.factorypulse.alert.application.port.out.InterventionRepository;
import com.factorypulse.alert.domain.Intervention;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/** ADAPTATEUR DE SORTIE : implémente le port {@link InterventionRepository} avec MongoDB. */
@Component
public class MongoInterventionRepository implements InterventionRepository {

    private final SpringDataInterventionRepository springData;

    public MongoInterventionRepository(SpringDataInterventionRepository springData) {
        this.springData = springData;
    }

    @Override
    public void save(Intervention intervention) {
        springData.save(InterventionDocument.from(intervention));
    }

    @Override
    public List<Intervention> findRecent(int limit) {
        return springData.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                .stream().map(InterventionDocument::toDomain).toList();
    }
}
