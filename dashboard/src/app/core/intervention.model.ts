/** Une intervention de maintenance (même format JSON que le record Java Intervention). */
export interface Intervention {
  id: string;
  machineId: string;
  alertId: string | null;
  cause: InterventionCause;
  description: string;
  durationMinutes: number;
  performedBy: string;
  createdAt: number;   // millisecondes epoch
}

export type InterventionCause = 'MECANIQUE' | 'ELECTRIQUE' | 'REGLAGE' | 'MATIERE' | 'AUTRE';

export const CAUSE_LABELS: Record<InterventionCause, string> = {
  MECANIQUE: 'Mécanique',
  ELECTRIQUE: 'Électrique',
  REGLAGE: 'Réglage',
  MATIERE: 'Matière première',
  AUTRE: 'Autre',
};

/** Ce que le formulaire envoie. L'opérateur n'y figure pas : le serveur le lit dans le jeton. */
export interface InterventionRequest {
  machineId: string;
  alertId: string | null;
  cause: InterventionCause;
  description: string;
  durationMinutes: number;
}

export const MAX_DESCRIPTION_LENGTH = 500;
export const MAX_DURATION_MINUTES = 24 * 60;
