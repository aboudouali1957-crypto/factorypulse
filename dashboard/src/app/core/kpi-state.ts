import { KpiSnapshot, MachineView, OeePoint } from './kpi.model';

/**
 * Logique pure de l'état du tableau de bord (aucune dépendance à Angular, HTTP ou WebSocket) :
 * facile à tester, comme le domaine côté Java.
 */

/** Nombre maximal de points gardés dans l'historique du TRS de chaque machine. */
export const MAX_HISTORY_POINTS = 120;

/** On n'ajoute un point à l'historique que si au moins 1 seconde s'est écoulée depuis le précédent. */
export const MIN_POINT_INTERVAL_MS = 1000;

/**
 * Intègre de nouveaux indicateurs dans l'état, sans modifier l'ancien (immutabilité).
 * Ignore une mise à jour plus ancienne que celle déjà connue (message arrivé en retard).
 */
export function applySnapshot(
  state: Record<string, MachineView>,
  snapshot: KpiSnapshot,
): Record<string, MachineView> {
  const previous = state[snapshot.machineId];

  if (previous && isOlder(snapshot, previous.snapshot)) {
    return state;
  }

  const history = previous ? appendPoint(previous.history, snapshot) : startHistory(snapshot);
  return { ...state, [snapshot.machineId]: { snapshot, history } };
}

function isOlder(candidate: KpiSnapshot, current: KpiSnapshot): boolean {
  return candidate.updatedAt != null && current.updatedAt != null && candidate.updatedAt < current.updatedAt;
}

function startHistory(snapshot: KpiSnapshot): OeePoint[] {
  return snapshot.updatedAt == null ? [] : [{ t: snapshot.updatedAt, oee: snapshot.oee }];
}

function appendPoint(history: OeePoint[], snapshot: KpiSnapshot): OeePoint[] {
  if (snapshot.updatedAt == null) {
    return history;
  }
  const last = history[history.length - 1];
  const point = { t: snapshot.updatedAt, oee: snapshot.oee };

  if (last && snapshot.updatedAt - last.t < MIN_POINT_INTERVAL_MS) {
    // Même seconde : on remplace le dernier point au lieu d'en ajouter un.
    return [...history.slice(0, -1), point];
  }
  return [...history, point].slice(-MAX_HISTORY_POINTS);
}

/** Les indicateurs de toute la ligne de production. */
export interface LineSummary {
  machineCount: number;
  machinesDown: number;
  averageOee: number;
  piecesProduced: number;
  rejectRate: number;
  energyKwh: number;
}

export function summarize(machines: MachineView[]): LineSummary {
  const snapshots = machines.map((m) => m.snapshot);
  const pieces = sum(snapshots.map((s) => s.piecesProduced));
  const rejected = sum(snapshots.map((s) => s.rejectedPieces));

  return {
    machineCount: snapshots.length,
    machinesDown: snapshots.filter((s) => !s.running).length,
    averageOee: snapshots.length === 0 ? 0 : sum(snapshots.map((s) => s.oee)) / snapshots.length,
    piecesProduced: pieces,
    rejectRate: pieces === 0 ? 0 : rejected / pieces,
    energyKwh: sum(snapshots.map((s) => s.energyKwh)),
  };
}

function sum(values: number[]): number {
  return values.reduce((total, v) => total + v, 0);
}

/** Niveau du TRS : seuils habituels dans l'industrie. */
export type OeeLevel = 'good' | 'warning' | 'critical';

export function oeeLevel(oee: number): OeeLevel {
  if (oee >= 0.85) {
    return 'good';
  }
  return oee >= 0.6 ? 'warning' : 'critical';
}

export const OEE_LEVEL_LABELS: Record<OeeLevel, string> = {
  good: 'Excellent',
  warning: 'À surveiller',
  critical: 'Insuffisant',
};
