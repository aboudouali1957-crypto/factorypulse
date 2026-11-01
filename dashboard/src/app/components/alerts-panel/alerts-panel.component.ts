import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { formatDuration } from '../../core/alert-state';
import { ALERT_TYPE_LABELS, SEVERITY_LABELS } from '../../core/alert.model';
import { AlertStore } from '../../core/alert-store.service';
import { formatTime } from '../../core/format';
import { MACHINE_NAMES } from '../../core/kpi.model';

/** Le panneau des alertes : les alertes actives (les plus urgentes en haut), puis les dernières résolues. */
@Component({
  selector: 'fp-alerts-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './alerts-panel.component.html',
  styleUrl: './alerts-panel.component.css',
})
export class AlertsPanelComponent {
  readonly store = inject(AlertStore);

  /** L'heure actuelle, rafraîchie chaque seconde, pour afficher depuis combien de temps une alerte dure. */
  readonly now = signal(Date.now());

  readonly disconnected = computed(() => this.store.connection() === 'disconnected');

  readonly typeLabels = ALERT_TYPE_LABELS;
  readonly severityLabels = SEVERITY_LABELS;
  readonly time = formatTime;

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  machineName(machineId: string): string {
    return MACHINE_NAMES[machineId] ?? machineId;
  }

  since(raisedAt: number): string {
    return formatDuration(this.now() - raisedAt);
  }
}
