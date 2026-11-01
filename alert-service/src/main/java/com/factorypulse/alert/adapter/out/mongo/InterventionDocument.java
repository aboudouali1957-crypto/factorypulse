package com.factorypulse.alert.adapter.out.mongo;

import com.factorypulse.alert.domain.Intervention;
import com.factorypulse.alert.domain.InterventionCause;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Le format d'une intervention dans MongoDB (collection "interventions"). */
@Document(collection = "interventions")
public class InterventionDocument {

    @Id
    private String id;
    @Indexed
    private String machineId;
    private String alertId;
    private InterventionCause cause;
    private String description;
    private int durationMinutes;
    private String performedBy;
    @Indexed
    private long createdAt;

    /** Constructeur vide, utilisé par Spring Data pour relire un document. */
    protected InterventionDocument() {
    }

    static InterventionDocument from(Intervention intervention) {
        InterventionDocument doc = new InterventionDocument();
        doc.id = intervention.id();
        doc.machineId = intervention.machineId();
        doc.alertId = intervention.alertId();
        doc.cause = intervention.cause();
        doc.description = intervention.description();
        doc.durationMinutes = intervention.durationMinutes();
        doc.performedBy = intervention.performedBy();
        doc.createdAt = intervention.createdAt();
        return doc;
    }

    Intervention toDomain() {
        return new Intervention(id, machineId, alertId, cause, description, durationMinutes, performedBy, createdAt);
    }
}
