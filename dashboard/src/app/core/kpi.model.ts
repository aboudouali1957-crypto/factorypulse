/** Les indicateurs d'une machine sur les 15 dernières minutes (record Java RecentKpi). */
export interface RecentKpi {
  windowMinutes: number;
  availability: number;      // 0 à 1
  performance: number;       // 0 à 1
  quality: number;           // 0 à 1
  oee: number;               // TRS, 0 à 1
  piecesProduced: number;
  rejectedPieces: number;
}

/**
 * Les indicateurs d'une machine, tels qu'envoyés par le kpi-service
 * (même format JSON que le record Java KpiSnapshot).
 * Les indicateurs à la racine sont cumulés depuis le début ; `recent` porte sur les 15 dernières minutes.
 */
export interface KpiSnapshot {
  machineId: string;
  running: boolean;
  availability: number;      // 0 à 1
  performance: number;       // 0 à 1
  quality: number;           // 0 à 1
  oee: number;               // TRS, 0 à 1
  piecesProduced: number;
  goodPieces: number;
  rejectedPieces: number;
  rejectRate: number;        // 0 à 1
  throughputPerHour: number;
  energyKwh: number;
  downCount: number;
  updatedAt: number | null;  // millisecondes epoch
  recent: RecentKpi;
}

/** Un point de l'historique du TRS (sur 15 min) d'une machine. */
export interface OeePoint {
  t: number;    // date en millisecondes epoch
  oee: number;  // 0 à 1
}

/** Ce que le tableau de bord connaît d'une machine : ses derniers indicateurs et l'historique du TRS. */
export interface MachineView {
  snapshot: KpiSnapshot;
  history: OeePoint[];
}

/** Noms lisibles des machines (le kpi-service ne connaît que les identifiants). */
export const MACHINE_NAMES: Record<string, string> = {
  M1: 'Presse',
  M2: 'Assemblage',
  M3: 'Conditionnement',
  M4: 'Contrôle qualité',
};

export type ConnectionState = 'connecting' | 'connected' | 'disconnected';
