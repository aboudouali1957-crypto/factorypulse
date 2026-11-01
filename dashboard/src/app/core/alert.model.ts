/** Une alerte, telle qu'envoyée par l'alert-service (même format JSON que le record Java Alert). */
export interface Alert {
  id: string;
  machineId: string;
  type: AlertType;
  severity: Severity;
  message: string;
  raisedAt: number;           // millisecondes epoch
  updatedAt: number;
  resolvedAt: number | null;  // null tant que l'alerte est active
  acknowledgedBy: string | null;  // opérateur qui a pris l'alerte en charge
  acknowledgedAt: number | null;
}

export type AlertType = 'DOWNTIME' | 'HIGH_REJECT_RATE';
export type Severity = 'WARNING' | 'CRITICAL';

export const ALERT_TYPE_LABELS: Record<AlertType, string> = {
  DOWNTIME: 'Panne',
  HIGH_REJECT_RATE: 'Rebuts',
};

export const SEVERITY_LABELS: Record<Severity, string> = {
  WARNING: 'À surveiller',
  CRITICAL: 'Critique',
};
