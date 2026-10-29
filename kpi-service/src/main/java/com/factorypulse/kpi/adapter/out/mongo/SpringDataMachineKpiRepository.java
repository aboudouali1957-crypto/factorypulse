package com.factorypulse.kpi.adapter.out.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository Spring Data : Spring génère automatiquement l'implémentation
 * (findById, findAll, save…) à partir de cette interface vide.
 */
interface SpringDataMachineKpiRepository extends MongoRepository<MachineKpiDocument, String> {
}
