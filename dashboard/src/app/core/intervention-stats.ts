import { InterventionCause, Intervention } from './intervention.model';

/** Bilan des interventions d'une machine, pour le responsable. */
export interface MachineInterventionStats {
  machineId: string;
  count: number;
  totalMinutes: number;
  mainCause: InterventionCause | null;   // la cause la plus fréquente
}

/** Regroupe les interventions par machine, de la machine qui a demandé le plus de temps à la moins sollicitée. */
export function statsByMachine(interventions: Intervention[]): MachineInterventionStats[] {
  const byMachine = new Map<string, Intervention[]>();
  for (const i of interventions) {
    byMachine.set(i.machineId, [...(byMachine.get(i.machineId) ?? []), i]);
  }
  return [...byMachine.entries()]
    .map(([machineId, list]) => ({
      machineId,
      count: list.length,
      totalMinutes: list.reduce((total, i) => total + i.durationMinutes, 0),
      mainCause: mostFrequentCause(list),
    }))
    .sort((a, b) => b.totalMinutes - a.totalMinutes || a.machineId.localeCompare(b.machineId));
}

function mostFrequentCause(list: Intervention[]): InterventionCause | null {
  const counts = new Map<InterventionCause, number>();
  for (const i of list) {
    counts.set(i.cause, (counts.get(i.cause) ?? 0) + 1);
  }
  let best: InterventionCause | null = null;
  for (const [cause, n] of counts) {
    if (best === null || n > (counts.get(best) ?? 0)) {
      best = cause;
    }
  }
  return best;
}

/** Durée lisible en minutes, ex. 75 → "1 h 15". */
export function formatMinutes(minutes: number): string {
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return m === 0 ? `${h} h` : `${h} h ${String(m).padStart(2, '0')}`;
}
