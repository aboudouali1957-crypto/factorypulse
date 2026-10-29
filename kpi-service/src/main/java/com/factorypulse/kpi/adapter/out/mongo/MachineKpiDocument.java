package com.factorypulse.kpi.adapter.out.mongo;

import com.factorypulse.kpi.domain.KpiSnapshot;
import com.factorypulse.kpi.domain.MachineKpi;
import com.factorypulse.kpi.domain.MachineKpiState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Le format d'une machine dans MongoDB (collection "machine_kpis"), un document par machine.
 *
 * On stocke :
 *  - les compteurs bruts, pour pouvoir reprendre le calcul après un redémarrage ;
 *  - les indicateurs calculés, pour pouvoir les lire directement dans MongoDB.
 *
 * Cette classe appartient à l'adaptateur : le domaine (MachineKpi) ne sait pas qu'elle existe.
 */
@Document(collection = "machine_kpis")
public class MachineKpiDocument {

    @Id
    private String machineId;

    // Compteurs bruts
    private double idealCycleSeconds;
    private boolean running;
    private Long lastTimestamp;
    private long runMillis;
    private long downMillis;
    private long piecesProduced;
    private long goodPieces;
    private double energyKwh;
    private long downCount;

    // Indicateurs calculés (en lecture seule pour MongoDB)
    private double availability;
    private double performance;
    private double quality;
    private double oee;
    private double rejectRate;
    private double throughputPerHour;

    /** Constructeur vide, utilisé par Spring Data pour relire un document. */
    protected MachineKpiDocument() {
    }

    public static MachineKpiDocument from(MachineKpi kpi) {
        MachineKpiState state = kpi.state();
        KpiSnapshot snapshot = kpi.snapshot();
        MachineKpiDocument doc = new MachineKpiDocument();
        doc.machineId = state.machineId();
        doc.idealCycleSeconds = state.idealCycleSeconds();
        doc.running = state.running();
        doc.lastTimestamp = state.lastTimestamp();
        doc.runMillis = state.runMillis();
        doc.downMillis = state.downMillis();
        doc.piecesProduced = state.piecesProduced();
        doc.goodPieces = state.goodPieces();
        doc.energyKwh = state.energyKwh();
        doc.downCount = state.downCount();
        doc.availability = snapshot.availability();
        doc.performance = snapshot.performance();
        doc.quality = snapshot.quality();
        doc.oee = snapshot.oee();
        doc.rejectRate = snapshot.rejectRate();
        doc.throughputPerHour = snapshot.throughputPerHour();
        return doc;
    }

    public MachineKpi toDomain() {
        return MachineKpi.restore(new MachineKpiState(machineId, idealCycleSeconds, running, lastTimestamp,
                runMillis, downMillis, piecesProduced, goodPieces, energyKwh, downCount));
    }

    public String getMachineId() {
        return machineId;
    }
}
