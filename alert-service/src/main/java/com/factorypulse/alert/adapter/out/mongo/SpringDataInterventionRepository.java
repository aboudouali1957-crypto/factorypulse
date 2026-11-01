package com.factorypulse.alert.adapter.out.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

interface SpringDataInterventionRepository extends MongoRepository<InterventionDocument, String> {

    List<InterventionDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
