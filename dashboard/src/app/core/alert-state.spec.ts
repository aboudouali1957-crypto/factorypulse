import { activeAlerts, activeCountByMachine, formatDuration, MAX_ALERTS, resolvedAlerts, upsertAlert } from './alert-state';
import { Alert } from './alert.model';

function alert(partial: Partial<Alert>): Alert {
  return {
    id: 'M1-DOWNTIME-1000', machineId: 'M1', type: 'DOWNTIME', severity: 'WARNING', message: 'M1 en panne',
    raisedAt: 1000, updatedAt: 1000, resolvedAt: null,
    ...partial,
  };
}

describe('upsertAlert', () => {
  it('remplace une alerte par sa nouvelle version (même id)', () => {
    let state = upsertAlert({}, alert({}));
    state = upsertAlert(state, alert({ severity: 'CRITICAL', updatedAt: 31_000 }));
    expect(Object.keys(state).length).toBe(1);
    expect(state['M1-DOWNTIME-1000'].severity).toBe('CRITICAL');
  });

  it('ignore une version plus ancienne que celle déjà connue', () => {
    let state = upsertAlert({}, alert({ updatedAt: 40_000, resolvedAt: 40_000 }));
    state = upsertAlert(state, alert({ updatedAt: 31_000, severity: 'CRITICAL' }));
    expect(state['M1-DOWNTIME-1000'].resolvedAt).toBe(40_000);
  });

  it(`garde au plus ${MAX_ALERTS} alertes, sans jamais perdre une alerte active`, () => {
    let state = upsertAlert({}, alert({ id: 'active', raisedAt: 0, updatedAt: 0 }));
    for (let i = 1; i <= MAX_ALERTS + 20; i++) {
      state = upsertAlert(state, alert({ id: `r${i}`, updatedAt: i, resolvedAt: i }));
    }
    expect(Object.keys(state).length).toBe(MAX_ALERTS);
    expect(state['active']).toBeDefined();
  });
});

describe('activeAlerts / resolvedAlerts', () => {
  const state = [
    alert({ id: 'a', raisedAt: 1000 }),
    alert({ id: 'b', raisedAt: 2000, severity: 'CRITICAL' }),
    alert({ id: 'c', raisedAt: 500 }),
    alert({ id: 'd', updatedAt: 5000, resolvedAt: 5000 }),
    alert({ id: 'e', updatedAt: 9000, resolvedAt: 9000 }),
  ].reduce(upsertAlert, {} as Record<string, Alert>);

  it('met les critiques en premier, puis les plus anciennes', () => {
    expect(activeAlerts(state).map((a) => a.id)).toEqual(['b', 'c', 'a']);
  });

  it('liste les résolues de la plus récente à la plus ancienne', () => {
    expect(resolvedAlerts(state, 10).map((a) => a.id)).toEqual(['e', 'd']);
    expect(resolvedAlerts(state, 1).length).toBe(1);
  });

  it('compte les alertes actives par machine', () => {
    const counts = activeCountByMachine(upsertAlert(state, alert({ id: 'f', machineId: 'M3' })));
    expect(counts).toEqual({ M1: 3, M3: 1 });
  });
});

describe('formatDuration', () => {
  it('affiche des secondes et des minutes', () => {
    expect(formatDuration(12_000)).toBe('12 s');
    expect(formatDuration(75_000)).toBe('1 min 15 s');
    expect(formatDuration(120_000)).toBe('2 min');
  });
});
