import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { formatPercent } from '../../core/format';

/** Jauge circulaire du TRS : l'anneau se remplit de 0 à 100 %. */
@Component({
  selector: 'fp-oee-gauge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 120 120" role="img" [attr.aria-label]="'TRS ' + label()">
      <circle class="track" cx="60" cy="60" [attr.r]="radius" />
      <circle class="value" cx="60" cy="60" [attr.r]="radius"
              [attr.stroke-dasharray]="circumference"
              [attr.stroke-dashoffset]="offset()" />
      <text x="60" y="60" class="pct num">{{ label() }}</text>
      <text x="60" y="80" class="caption">TRS</text>
    </svg>
  `,
  styles: `
    :host { display: block; width: 128px; }
    svg { width: 100%; height: auto; display: block; }
    circle { fill: none; stroke-width: 10; }
    .track { stroke: var(--surface-2); }
    .value { stroke: var(--series-1); stroke-linecap: round; transform: rotate(-90deg); transform-origin: 60px 60px;
             transition: stroke-dashoffset 400ms ease; }
    text { text-anchor: middle; fill: var(--text-primary); }
    .pct { font-size: 20px; font-weight: 600; dominant-baseline: middle; }
    .caption { font-size: 11px; fill: var(--text-muted); letter-spacing: .08em; }
  `,
})
export class OeeGaugeComponent {
  /** Le TRS, entre 0 et 1. */
  readonly value = input.required<number>();

  readonly radius = 50;
  readonly circumference = 2 * Math.PI * this.radius;

  readonly offset = computed(() => this.circumference * (1 - clamp(this.value())));
  readonly label = computed(() => formatPercent(this.value()));
}

function clamp(v: number): number {
  return Math.max(0, Math.min(1, v));
}
