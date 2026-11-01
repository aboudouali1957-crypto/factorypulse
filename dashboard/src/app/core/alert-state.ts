import { Alert } from './alert.model';

/**
 * Logique pure de la liste des alertes (aucune dépendance à Angular, HTTP ou WebSocket).
 *
 * L'état est une Map "identifiant → dernière version de l'alerte" : quand une alerte est
 * aggravée ou résolue, l'alert-service renvoie la même alerte (même id) dans sa nouvelle version.
 */

/** Nombre maximum d'alertes gardées en mémoire dans le navigateur. */
export const MAX_ALERTS = 100;

export function isActive(alert: Alert): boolean {
  return alert.resolvedAt == null;
}

/** Ajoute ou remplace une alerte. Ignore une version plus ancienne que celle déjà connue. */
export function upsertAlert(state: Record<string, Alert>, alert: Alert): Record<string, Alert> {
  const previous = state[alert.id];
  if (previous && alert.updatedAt < previous.updatedAt) {
    return state;
  }
  const next = { ...state, [alert.id]: alert };
  return trim(next);
}

/** Garde les alertes actives et les plus récentes, pour ne pas grossir indéfiniment. */
function trim(state: Record<string, Alert>): Record<string, Alert> {
  const all = Object.values(state);
  if (all.length <= MAX_ALERTS) {
    return state;
  }
  const kept = [...all.filter(isActive), ...sortByMostRecent(all.filter((a) => !isActive(a)))].slice(0, MAX_ALERTS);
  return Object.fromEntries(kept.map((a) => [a.id, a]));
}

/** Les alertes actives : les critiques d'abord, puis les plus anciennes (les plus urgentes) en premier. */
export function activeAlerts(state: Record<string, Alert>): Alert[] {
  return Object.values(state)
    .filter(isActive)
    .sort((a, b) => severityRank(b) - severityRank(a) || a.raisedAt - b.raisedAt);
}

/** Les alertes résolues, de la plus récente à la plus ancienne. */
export function resolvedAlerts(state: Record<string, Alert>, limit: number): Alert[] {
  return sortByMostRecent(Object.values(state).filter((a) => !isActive(a))).slice(0, limit);
}

/** Nombre d'alertes actives par machine (pour un badge sur la carte de la machine). */
export function activeCountByMachine(state: Record<string, Alert>): Partial<Record<string, number>> {
  const counts: Partial<Record<string, number>> = {};
  for (const alert of Object.values(state).filter(isActive)) {
    counts[alert.machineId] = (counts[alert.machineId] ?? 0) + 1;
  }
  return counts;
}

function sortByMostRecent(alerts: Alert[]): Alert[] {
  return [...alerts].sort((a, b) => b.updatedAt - a.updatedAt);
}

function severityRank(alert: Alert): number {
  return alert.severity === 'CRITICAL' ? 1 : 0;
}

/** Durée lisible, ex. 75 000 ms → "1 min 15 s". */
export function formatDuration(millis: number): string {
  const totalSeconds = Math.max(0, Math.round(millis / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) {
    return `${seconds} s`;
  }
  return seconds === 0 ? `${minutes} min` : `${minutes} min ${seconds} s`;
}
