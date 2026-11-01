package com.factorypulse.alert.adapter.out.mongo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository Spring Data : Spring génère les requêtes MongoDB à partir du NOM des méthodes.
 * Par exemple, findByResolvedAtIsNull → { "resolvedAt": null }.
 */
interface SpringDataAlertRepository extends MongoRepository<AlertDocument, String> {

    List<AlertDocument> findByResolvedAtIsNullOrderByRaisedAtDesc();

    List<AlertDocument> findByMachineIdAndResolvedAtIsNull(String machineId);

    List<AlertDocument> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
