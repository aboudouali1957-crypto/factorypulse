package com.factorypulse.kpi.adapter.out.mongo;

import com.factorypulse.kpi.domain.KpiSnapshot;
import com.factorypulse.kpi.domain.MachineKpi;
import com.factorypulse.kpi.domain.MachineKpiState;
import com.factorypulse.kpi.domain.MinuteBucket;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Le format d'une machine dans MongoDB (collection "machine_kpis"), un document par machine.
 *
 * On stocke :
 *  - les compteurs bruts, pour pouvoir reprendre le calcul après un redémarrage
 *    (dont les compteurs minute par minute des 15 dernières minutes) ;
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
    private List<MinuteDocument> recentMinutes;

    // Indicateurs calculés (en lecture seule pour MongoDB)
    private double availability;
    private double performance;
    private double quality;
    private double oee;
    private double rejectRate;
    private double throughputPerHour;
    private double recentOee;

    /** Les compteurs d'une minute, tels que stockés dans MongoDB. */
    public static class MinuteDocument {
        private long minute;
        private long runMillis;
        private long downMillis;
        private long piecesProduced;
        private long goodPieces;

        protected MinuteDocument() {
        }

        static MinuteDocument from(MinuteBucket bucket) {
            MinuteDocument doc = new MinuteDocument();
            doc.minute = bucket.minute();
            doc.runMillis = bucket.runMillis();
            doc.downMillis = bucket.downMillis();
            doc.piecesProduced = bucket.piecesProduced();
            doc.goodPieces = bucket.goodPieces();
            return doc;
        }

        MinuteBucket toDomain() {
            return new MinuteBucket(minute, runMillis, downMillis, piecesProduced, goodPieces);
        }
    }

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
        doc.recentMinutes = state.recentMinutes().stream().map(MinuteDocument::from).toList();
        doc.availability = snapshot.availability();
        doc.performance = snapshot.performance();
        doc.quality = snapshot.quality();
        doc.oee = snapshot.oee();
        doc.rejectRate = snapshot.rejectRate();
        doc.throughputPerHour = snapshot.throughputPerHour();
        doc.recentOee = snapshot.recent().oee();
        return doc;
    }

    public MachineKpi toDomain() {
        // Les documents enregistrés avant le jour 4 n'ont pas encore de compteurs par minute.
        List<MinuteBucket> minutes = recentMinutes == null
                ? List.of()
                : recentMinutes.stream().map(MinuteDocument::toDomain).toList();
        return MachineKpi.restore(new MachineKpiState(machineId, idealCycleSeconds, running, lastTimestamp,
                runMillis, downMillis, piecesProduced, goodPieces, energyKwh, downCount, minutes));
    }

    public String getMachineId() {
        return machineId;
    }
}
