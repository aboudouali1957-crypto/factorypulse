import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { formatDuration } from '../../core/alert-state';
import { Alert, ALERT_TYPE_LABELS, SEVERITY_LABELS } from '../../core/alert.model';
import { AlertStore } from '../../core/alert-store.service';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/intervention.service';
import { InterventionFormComponent } from '../intervention-form/intervention-form.component';
import { formatTime } from '../../core/format';
import { MACHINE_NAMES } from '../../core/kpi.model';

/**
 * Le panneau des alertes : les alertes actives (les plus urgentes en haut), puis les dernières résolues.
 * L'opérateur peut y prendre une alerte en charge et saisir une intervention.
 */
@Component({
  selector: 'fp-alerts-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [InterventionFormComponent],
  templateUrl: './alerts-panel.component.html',
  styleUrl: './alerts-panel.component.css',
})
export class AlertsPanelComponent {
  readonly store = inject(AlertStore);
  readonly auth = inject(AuthService);

  /** Seul un opérateur voit les boutons d'action (le serveur vérifie aussi le rôle). */
  readonly canAct = this.auth.hasRole('OPERATEUR');

  /** Alerte en cours d'acquittement (bouton désactivé pendant l'appel). */
  readonly pending = signal<string | null>(null);
  /** Message de confirmation ou d'erreur, affiché quelques secondes. */
  readonly notice = signal<{ text: string; error: boolean } | null>(null);
  /** Formulaire d'intervention ouvert : machine et alerte pré-remplies (ou null si fermé). */
  readonly formTarget = signal<{ machineId: string; alertId: string | null } | null>(null);
  private noticeTimer?: ReturnType<typeof setTimeout>;

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

  acknowledge(alert: Alert): void {
    this.pending.set(alert.id);
    this.store.acknowledge(alert.id).subscribe({
      next: () => {
        this.pending.set(null);
        this.show(`Alerte ${alert.machineId} prise en charge.`, false);
      },
      error: (e: HttpErrorResponse) => {
        this.pending.set(null);
        this.show(errorMessage(e), true);
      },
    });
  }

  openForm(machineId = 'M1', alertId: string | null = null): void {
    this.formTarget.set({ machineId, alertId });
  }

  onSaved(): void {
    this.show('Intervention enregistrée.', false);
  }

  private show(text: string, error: boolean): void {
    this.notice.set({ text, error });
    clearTimeout(this.noticeTimer);
    this.noticeTimer = setTimeout(() => this.notice.set(null), 5000);
  }
}
