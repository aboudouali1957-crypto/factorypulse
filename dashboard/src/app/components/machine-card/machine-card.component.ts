import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatDecimal, formatInteger, formatPercent } from '../../core/format';
import { OEE_LEVEL_LABELS, oeeLevel } from '../../core/kpi-state';
import { MACHINE_NAMES, MachineView } from '../../core/kpi.model';
import { OeeGaugeComponent } from '../oee-gauge/oee-gauge.component';
import { SparklineComponent } from '../sparkline/sparkline.component';

/**
 * La carte d'une machine : état, jauge du TRS, ses trois composantes, compteurs et évolution.
 * La jauge, les barres et la courbe portent sur les 15 dernières minutes ; les compteurs sur toute la durée.
 */
@Component({
  selector: 'fp-machine-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [OeeGaugeComponent, SparklineComponent],
  templateUrl: './machine-card.component.html',
  styleUrl: './machine-card.component.css',
})
export class MachineCardComponent {
  readonly machine = input.required<MachineView>();
  /** Nombre d'alertes actives sur cette machine (fourni par l'alert-service). */
  readonly alertCount = input(0);

  readonly s = computed(() => this.machine().snapshot);
  /** Les indicateurs des 15 dernières minutes : ce qu'on affiche en grand. */
  readonly recent = computed(() => this.s().recent);
  readonly name = computed(() => MACHINE_NAMES[this.s().machineId] ?? this.s().machineId);
  readonly level = computed(() => oeeLevel(this.recent().oee));
  readonly levelLabel = computed(() => OEE_LEVEL_LABELS[this.level()]);

  /** Les trois composantes du TRS sur 15 min, affichées en barres. */
  readonly factors = computed(() => [
    { label: 'Disponibilité', value: this.recent().availability },
    { label: 'Performance', value: this.recent().performance },
    { label: 'Qualité', value: this.recent().quality },
  ]);

  readonly percent = formatPercent;
  readonly integer = formatInteger;
  readonly decimal = formatDecimal;
}
