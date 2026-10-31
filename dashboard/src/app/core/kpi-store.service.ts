import { HttpClient } from '@angular/common/http';
import { computed, Injectable, OnDestroy, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { applySnapshot, summarize } from './kpi-state';
import { ConnectionState, KpiSnapshot, MachineView } from './kpi.model';

/**
 * L'état du tableau de bord, partagé par tous les composants.
 *
 *  1. Au démarrage : charge les indicateurs actuels avec l'API REST (GET /api/kpis).
 *  2. Ensuite : s'abonne au WebSocket (STOMP, destination /topic/kpis) et reçoit chaque
 *     mise à jour en direct, sans recharger la page.
 *
 * L'état est stocké dans des "signals" Angular : quand il change, l'affichage se met à jour tout seul.
 */
@Injectable({ providedIn: 'root' })
export class KpiStore implements OnDestroy {
  private readonly machinesById = signal<Record<string, MachineView>>({});

  /** Les machines, triées par identifiant (M1, M2…). */
  readonly machines = computed(() =>
    Object.values(this.machinesById()).sort((a, b) => a.snapshot.machineId.localeCompare(b.snapshot.machineId)),
  );

  /** Les indicateurs de toute la ligne. */
  readonly summary = computed(() => summarize(this.machines()));

  readonly connection = signal<ConnectionState>('connecting');
  readonly lastUpdate = signal<number | null>(null);

  private readonly stomp: Client;

  constructor(private readonly http: HttpClient) {
    this.loadInitialState();

    this.stomp = new Client({
      brokerURL: webSocketUrl(),
      reconnectDelay: 3000,                    // reconnexion automatique toutes les 3 s si le service redémarre
      onConnect: () => {
        this.connection.set('connected');
        this.stomp.subscribe('/topic/kpis', (message) => this.receive(JSON.parse(message.body) as KpiSnapshot));
      },
      onWebSocketClose: () => this.connection.set('disconnected'),
      onStompError: () => this.connection.set('disconnected'),
    });
    this.stomp.activate();
  }

  private loadInitialState(): void {
    this.http.get<KpiSnapshot[]>('/api/kpis').subscribe({
      next: (snapshots) => snapshots.forEach((s) => this.receive(s)),
      error: () => this.connection.set('disconnected'),
    });
  }

  private receive(snapshot: KpiSnapshot): void {
    this.machinesById.update((state) => applySnapshot(state, snapshot));
    if (snapshot.updatedAt != null) {
      this.lastUpdate.set(snapshot.updatedAt);
    }
  }

  ngOnDestroy(): void {
    void this.stomp.deactivate();
  }
}

/** Même hôte que la page : en local, un proxy (ng serve ou nginx) redirige /ws vers le kpi-service. */
function webSocketUrl(): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${location.host}/ws`;
}
