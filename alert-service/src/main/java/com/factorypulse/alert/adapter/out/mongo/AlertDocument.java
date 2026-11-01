package com.factorypulse.alert.adapter.out.mongo;

import com.factorypulse.alert.domain.Alert;
import com.factorypulse.alert.domain.AlertType;
import com.factorypulse.alert.domain.Severity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Le format d'une alerte dans MongoDB (collection "alerts"), un document par alerte.
 *
 * Quand une alerte change (aggravée, résolue), on enregistre un document avec le même identifiant :
 * MongoDB remplace l'ancienne version.
 */
@Document(collection = "alerts")
public class AlertDocument {

    @Id
    private String id;
    private String machineId;
    private AlertType type;
    private Severity severity;
    private String message;
    private long raisedAt;
    @Indexed
    private long updatedAt;
    @Indexed
    private Long resolvedAt;
    private String acknowledgedBy;
    private Long acknowledgedAt;

    /** Constructeur vide, utilisé par Spring Data pour relire un document. */
    protected AlertDocument() {
    }

    static AlertDocument from(Alert alert) {
        AlertDocument doc = new AlertDocument();
        doc.id = alert.id();
        doc.machineId = alert.machineId();
        doc.type = alert.type();
        doc.severity = alert.severity();
        doc.message = alert.message();
        doc.raisedAt = alert.raisedAt();
        doc.updatedAt = alert.updatedAt();
        doc.resolvedAt = alert.resolvedAt();
        doc.acknowledgedBy = alert.acknowledgedBy();
        doc.acknowledgedAt = alert.acknowledgedAt();
        return doc;
    }

    Alert toDomain() {
        return new Alert(id, machineId, type, severity, message, raisedAt, updatedAt, resolvedAt,
                acknowledgedBy, acknowledgedAt);
    }
}
