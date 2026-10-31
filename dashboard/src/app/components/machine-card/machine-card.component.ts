import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatDecimal, formatInteger, formatPercent } from '../../core/format';
import { OEE_LEVEL_LABELS, oeeLevel } from '../../core/kpi-state';
import { MACHINE_NAMES, MachineView } from '../../core/kpi.model';
import { OeeGaugeComponent } from '../oee-gauge/oee-gauge.component';
import { SparklineComponent } from '../sparkline/sparkline.component';

/** La carte d'une machine : état, jauge du TRS, ses trois composantes, compteurs et évolution. */
@Component({
  selector: 'fp-machine-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [OeeGaugeComponent, SparklineComponent],
  templateUrl: './machine-card.component.html',
  styleUrl: './machine-card.component.css',
})
export class MachineCardComponent {
  readonly machine = input.required<MachineView>();

  readonly s = computed(() => this.machine().snapshot);
  readonly name = computed(() => MACHINE_NAMES[this.s().machineId] ?? this.s().machineId);
  readonly level = computed(() => oeeLevel(this.s().oee));
  readonly levelLabel = computed(() => OEE_LEVEL_LABELS[this.level()]);

  /** Les trois composantes du TRS, affichées en barres. */
  readonly factors = computed(() => [
    { label: 'Disponibilité', value: this.s().availability },
    { label: 'Performance', value: this.s().performance },
    { label: 'Qualité', value: this.s().quality },
  ]);

  readonly percent = formatPercent;
  readonly integer = formatInteger;
  readonly decimal = formatDecimal;
}
