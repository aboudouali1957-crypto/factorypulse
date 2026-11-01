import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, OnDestroy, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Observable, tap } from 'rxjs';
import { activeAlerts, activeCountByMachine, resolvedAlerts, upsertAlert } from './alert-state';
import { Alert } from './alert.model';
import { ConnectionState } from './kpi.model';
import { AuthService } from './auth.service';
import { sameHostWebSocketUrl } from './websocket-url';

/** Nombre d'alertes résolues affichées dans l'historique. */
const RESOLVED_SHOWN = 8;

/**
 * Les alertes, partagées par tous les composants. Même principe que {@link KpiStore},
 * mais avec l'alert-service :
 *
 *  1. Au démarrage : charge les dernières alertes avec l'API REST (GET /api/alerts).
 *  2. Ensuite : s'abonne au WebSocket (/ws-alerts, destination /topic/alerts).
 */
@Injectable({ providedIn: 'root' })
export class AlertStore implements OnDestroy {
  private readonly alertsById = signal<Record<string, Alert>>({});

  readonly active = computed(() => activeAlerts(this.alertsById()));
  readonly resolved = computed(() => resolvedAlerts(this.alertsById(), RESOLVED_SHOWN));
  readonly activeByMachine = computed(() => activeCountByMachine(this.alertsById()));
  readonly criticalCount = computed(() => this.active().filter((a) => a.severity === 'CRITICAL').length);

  readonly connection = signal<ConnectionState>('connecting');

  private readonly stomp: Client;
  private readonly auth = inject(AuthService);

  constructor(private readonly http: HttpClient) {
    this.loadInitialState();

    this.stomp = new Client({
      brokerURL: sameHostWebSocketUrl('/ws-alerts'),
      // Avant chaque (re)connexion : un jeton à jour, envoyé dans la trame STOMP CONNECT.
      beforeConnect: async (client) => {
        client.connectHeaders = { Authorization: `Bearer ${await this.auth.token()}` };
      },
      reconnectDelay: 3000,
      onConnect: () => {
        this.connection.set('connected');
        this.stomp.subscribe('/topic/alerts', (message) => this.receive(JSON.parse(message.body) as Alert));
      },
      onWebSocketClose: () => this.connection.set('disconnected'),
      onStompError: () => this.connection.set('disconnected'),
    });
    this.stomp.activate();
  }

  private loadInitialState(): void {
    this.http.get<Alert[]>('/api/alerts?limit=50').subscribe({
      next: (alerts) => alerts.forEach((a) => this.receive(a)),
      error: () => this.connection.set('disconnected'),
    });
  }

  /** Prise en charge d'une alerte (rôle OPERATEUR). Le serveur renvoie la nouvelle version de l'alerte. */
  acknowledge(alertId: string): Observable<Alert> {
    return this.http
      .post<Alert>(`/api/alerts/${encodeURIComponent(alertId)}/acknowledge`, null)
      .pipe(tap((alert) => this.receive(alert)));
  }

  private receive(alert: Alert): void {
    this.alertsById.update((state) => upsertAlert(state, alert));
  }

  ngOnDestroy(): void {
    void this.stomp.deactivate();
  }
}
