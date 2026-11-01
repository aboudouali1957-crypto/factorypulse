import { formatMinutes, statsByMachine } from './intervention-stats';
import { Intervention } from './intervention.model';

function intervention(partial: Partial<Intervention>): Intervention {
  return {
    id: 'i', machineId: 'M1', alertId: null, cause: 'MECANIQUE', description: 'x',
    durationMinutes: 10, performedBy: 'operateur', createdAt: 0,
    ...partial,
  };
}

describe('statsByMachine', () => {
  it('additionne les interventions par machine, la plus coûteuse en premier', () => {
    const stats = statsByMachine([
      intervention({ machineId: 'M1', durationMinutes: 10, cause: 'REGLAGE' }),
      intervention({ machineId: 'M2', durationMinutes: 45 }),
      intervention({ machineId: 'M1', durationMinutes: 20, cause: 'REGLAGE' }),
      intervention({ machineId: 'M1', durationMinutes: 5, cause: 'ELECTRIQUE' }),
    ]);
    expect(stats.map((s) => s.machineId)).toEqual(['M2', 'M1']);
    expect(stats[1]).toEqual({ machineId: 'M1', count: 3, totalMinutes: 35, mainCause: 'REGLAGE' });
  });

  it('renvoie une liste vide sans intervention', () => {
    expect(statsByMachine([])).toEqual([]);
  });
});

describe('formatMinutes', () => {
  it('affiche minutes et heures', () => {
    expect(formatMinutes(45)).toBe('45 min');
    expect(formatMinutes(60)).toBe('1 h');
    expect(formatMinutes(75)).toBe('1 h 15');
  });
});
