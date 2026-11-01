import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { statsByMachine } from './intervention-stats';
import { Intervention, InterventionRequest } from './intervention.model';

/** Les interventions de maintenance : saisie (opérateur) et historique (responsable). */
@Injectable({ providedIn: 'root' })
export class InterventionService {
  private readonly http = inject(HttpClient);

  readonly history = signal<Intervention[]>([]);
  readonly stats = computed(() => statsByMachine(this.history()));
  readonly error = signal<string | null>(null);
  /** Chargement en cours (pour afficher « Actualisation… » sur le bouton). */
  readonly loading = signal(false);
  /** Heure du dernier chargement réussi (millisecondes epoch). */
  readonly lastLoaded = signal<number | null>(null);

  create(request: InterventionRequest): Observable<Intervention> {
    return this.http.post<Intervention>('/api/interventions', request);
  }

  /** Recharge l'historique (rôle RESPONSABLE : sinon le serveur répond 403). */
  loadHistory(): void {
    if (this.loading()) {
      return;   // un chargement est déjà en cours
    }
    this.loading.set(true);
    this.http.get<Intervention[]>('/api/interventions?limit=100').subscribe({
      next: (list) => {
        this.history.set(list);
        this.error.set(null);
        this.lastLoaded.set(Date.now());
        this.loading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}

/** Message lisible à partir d'une erreur de l'API (format Problem Details : champ "detail"). */
export function errorMessage(error: HttpErrorResponse): string {
  if (error.status === 403) {
    return 'Action non autorisée pour votre rôle.';
  }
  if (error.status === 0) {
    return 'Service injoignable. Réessayez dans un instant.';
  }
  const detail = (error.error as { detail?: string } | null)?.detail;
  return detail ?? `Erreur ${error.status}`;
}

