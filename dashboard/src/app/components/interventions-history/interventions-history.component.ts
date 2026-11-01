import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { formatTime } from '../../core/format';
import { formatMinutes } from '../../core/intervention-stats';
import { CAUSE_LABELS } from '../../core/intervention.model';
import { InterventionService } from '../../core/intervention.service';
import { MACHINE_NAMES } from '../../core/kpi.model';

/** Rafraîchissement de l'historique, en millisecondes. */
const REFRESH_MS = 30_000;

/** L'historique des interventions et le bilan par machine (rôle RESPONSABLE). */
@Component({
  selector: 'fp-interventions-history',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './interventions-history.component.html',
  styleUrl: './interventions-history.component.css',
})
export class InterventionsHistoryComponent implements OnInit {
  readonly service = inject(InterventionService);
  private readonly destroyRef = inject(DestroyRef);

  readonly causeLabels = CAUSE_LABELS;
  readonly minutes = formatMinutes;
  readonly time = formatTime;
  private readonly dateFormat = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'short', timeStyle: 'short' });

  ngOnInit(): void {
    this.service.loadHistory();
    const timer = setInterval(() => this.service.loadHistory(), REFRESH_MS);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }

  machineName(id: string): string {
    return MACHINE_NAMES[id] ?? id;
  }

  date(epochMillis: number): string {
    return this.dateFormat.format(new Date(epochMillis));
  }
}
