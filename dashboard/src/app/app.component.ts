import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MachineCardComponent } from './components/machine-card/machine-card.component';
import { StatTileComponent } from './components/stat-tile/stat-tile.component';
import { formatDecimal, formatInteger, formatPercent, formatTime } from './core/format';
import { KpiStore } from './core/kpi-store.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MachineCardComponent, StatTileComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  readonly store = inject(KpiStore);

  readonly summary = this.store.summary;
  readonly machines = this.store.machines;

  readonly connectionLabel = computed(() => {
    switch (this.store.connection()) {
      case 'connected':
        return 'Temps réel';
      case 'connecting':
        return 'Connexion…';
      default:
        return 'Déconnecté – reconnexion…';
    }
  });

  readonly lastUpdate = computed(() => formatTime(this.store.lastUpdate()));

  readonly percent = formatPercent;
  readonly integer = formatInteger;
  readonly decimal = formatDecimal;
}
