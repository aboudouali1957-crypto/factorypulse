package com.factorypulse.kpi.domain;

/**
 * Les compteurs d'une machine pendant UNE minute.
 *
 * @param minute         numéro de la minute depuis l'epoch (timestamp / 60 000)
 * @param runMillis      temps de marche pendant cette minute
 * @param downMillis     temps d'arrêt pendant cette minute
 * @param piecesProduced pièces produites pendant cette minute
 * @param goodPieces     pièces bonnes pendant cette minute
 */
public record MinuteBucket(long minute, long runMillis, long downMillis, long piecesProduced, long goodPieces) {

    static MinuteBucket empty(long minute) {
        return new MinuteBucket(minute, 0, 0, 0, 0);
    }

    MinuteBucket plusRun(long millis) {
        return new MinuteBucket(minute, runMillis + millis, downMillis, piecesProduced, goodPieces);
    }

    MinuteBucket plusDown(long millis) {
        return new MinuteBucket(minute, runMillis, downMillis + millis, piecesProduced, goodPieces);
    }

    MinuteBucket plusPiece(boolean good) {
        return new MinuteBucket(minute, runMillis, downMillis, piecesProduced + 1, goodPieces + (good ? 1 : 0));
    }
}
