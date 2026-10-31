import { applySnapshot, MAX_HISTORY_POINTS, oeeLevel, summarize } from './kpi-state';
import { KpiSnapshot, MachineView } from './kpi.model';

function snapshot(partial: Partial<KpiSnapshot>): KpiSnapshot {
  return {
    machineId: 'M1', running: true, availability: 0.9, performance: 0.9, quality: 0.95, oee: 0.77,
    piecesProduced: 100, goodPieces: 97, rejectedPieces: 3, rejectRate: 0.03, throughputPerHour: 1500,
    energyKwh: 2, downCount: 1, updatedAt: 1_000_000,
    ...partial,
  };
}

describe('applySnapshot', () => {
  it('crée la machine au premier message, avec un point d\'historique', () => {
    const state = applySnapshot({}, snapshot({}));
    expect(state['M1'].snapshot.oee).toBe(0.77);
    expect(state['M1'].history.length).toBe(1);
  });

  it('ajoute un point par seconde, et remplace le dernier dans la même seconde', () => {
    let state = applySnapshot({}, snapshot({ updatedAt: 1_000_000 }));
    state = applySnapshot(state, snapshot({ updatedAt: 1_000_400, oee: 0.8 }));   // même seconde
    expect(state['M1'].history.length).toBe(1);
    expect(state['M1'].history[0].oee).toBe(0.8);

    state = applySnapshot(state, snapshot({ updatedAt: 1_001_500, oee: 0.82 }));  // seconde suivante
    expect(state['M1'].history.length).toBe(2);
  });

  it('ignore un message plus ancien que celui déjà reçu', () => {
    let state = applySnapshot({}, snapshot({ updatedAt: 2_000_000, oee: 0.9 }));
    state = applySnapshot(state, snapshot({ updatedAt: 1_000_000, oee: 0.1 }));
    expect(state['M1'].snapshot.oee).toBe(0.9);
  });

  it(`garde au plus ${MAX_HISTORY_POINTS} points`, () => {
    let state: Record<string, MachineView> = {};
    for (let i = 0; i < MAX_HISTORY_POINTS + 30; i++) {
      state = applySnapshot(state, snapshot({ updatedAt: i * 1000 }));
    }
    expect(state['M1'].history.length).toBe(MAX_HISTORY_POINTS);
  });
});

describe('summarize', () => {
  it('calcule les indicateurs de la ligne', () => {
    const machines: MachineView[] = [
      { snapshot: snapshot({ machineId: 'M1', oee: 0.8, piecesProduced: 100, rejectedPieces: 4, energyKwh: 1 }), history: [] },
      { snapshot: snapshot({ machineId: 'M2', oee: 0.6, piecesProduced: 300, rejectedPieces: 4, energyKwh: 2, running: false }), history: [] },
    ];
    const s = summarize(machines);
    expect(s.averageOee).toBeCloseTo(0.7, 10);
    expect(s.piecesProduced).toBe(400);
    expect(s.rejectRate).toBeCloseTo(0.02, 10);    // 8 rebuts sur 400 pièces
    expect(s.machinesDown).toBe(1);
    expect(s.energyKwh).toBe(3);
  });

  it('renvoie des zéros sans machine', () => {
    expect(summarize([]).averageOee).toBe(0);
  });
});

describe('oeeLevel', () => {
  it('classe le TRS selon les seuils habituels', () => {
    expect(oeeLevel(0.9)).toBe('good');
    expect(oeeLevel(0.7)).toBe('warning');
    expect(oeeLevel(0.4)).toBe('critical');
  });
});
