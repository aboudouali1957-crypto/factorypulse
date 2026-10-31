import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { formatPercent, formatTime } from '../../core/format';
import { OeePoint } from '../../core/kpi.model';

const WIDTH = 300;
const HEIGHT = 64;
const TARGET = 0.85;   // objectif de TRS, tracé en pointillés

/**
 * Courbe de l'évolution du TRS. Au survol, une ligne verticale et une infobulle
 * indiquent l'heure et la valeur du point le plus proche.
 */
@Component({
  selector: 'fp-sparkline',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="wrap" (pointermove)="onMove($event)" (pointerleave)="hover.set(null)">
      <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" preserveAspectRatio="none" role="img"
           [attr.aria-label]="'Évolution du TRS, ' + points().length + ' points'">
        <line class="target" x1="0" [attr.x2]="width" [attr.y1]="targetY()" [attr.y2]="targetY()" />
        <polyline class="line" [attr.points]="path()" />
        @if (hovered(); as h) {
          <line class="cursor" [attr.x1]="h.x" [attr.x2]="h.x" y1="0" [attr.y2]="height" />
        }
      </svg>
      @if (hovered(); as h) {
        <span class="dot" [style.left.%]="h.x / width * 100" [style.top.%]="h.y / height * 100"></span>
        <div class="tip num" [style.left.%]="h.x / width * 100">
          <strong>{{ h.value }}</strong> · {{ h.time }}
        </div>
      }
      <span class="target-label">Objectif 85 %</span>
    </div>
  `,
  styles: `
    :host { display: block; }
    .wrap { position: relative; height: 64px; touch-action: none; }
    svg { width: 100%; height: 100%; display: block; overflow: visible; }
    .line { fill: none; stroke: var(--series-1); stroke-width: 2; vector-effect: non-scaling-stroke;
            stroke-linejoin: round; stroke-linecap: round; }
    .target { stroke: var(--text-muted); stroke-width: 1; stroke-dasharray: 4 4; vector-effect: non-scaling-stroke; }
    .cursor { stroke: var(--text-secondary); stroke-width: 1; vector-effect: non-scaling-stroke; }
    .dot { position: absolute; width: 8px; height: 8px; margin: -4px 0 0 -4px; border-radius: 50%;
           background: var(--series-1); box-shadow: 0 0 0 2px var(--surface-1); pointer-events: none; }
    .tip { position: absolute; bottom: calc(100% + 6px); transform: translateX(-50%); white-space: nowrap;
           background: var(--surface-0); border: 1px solid var(--border); border-radius: 6px; padding: 3px 8px;
           font-size: 12px; color: var(--text-secondary); pointer-events: none; z-index: 1; }
    .tip strong { color: var(--text-primary); }
    .target-label { position: absolute; right: 0; top: -16px; font-size: 10px; color: var(--text-muted); }
  `,
})
export class SparklineComponent {
  readonly points = input.required<OeePoint[]>();

  readonly width = WIDTH;
  readonly height = HEIGHT;
  readonly hover = signal<number | null>(null);

  /**
   * Échelle verticale resserrée autour des valeurs (et de l'objectif), avec une petite marge :
   * sinon, un TRS qui varie entre 80 et 84 % donnerait une ligne plate.
   */
  private readonly domain = computed(() => {
    const values = [...this.points().map((p) => p.oee), TARGET];
    const min = Math.max(0, Math.min(...values) - 0.03);
    const max = Math.min(1, Math.max(...values) + 0.03);
    return { min, max: max > min ? max : min + 0.01 };
  });

  private y(oee: number): number {
    const { min, max } = this.domain();
    return HEIGHT - ((oee - min) / (max - min)) * HEIGHT;
  }

  private x(index: number): number {
    const n = this.points().length;
    return n <= 1 ? WIDTH : (index / (n - 1)) * WIDTH;
  }

  readonly targetY = computed(() => this.y(TARGET));

  readonly path = computed(() => this.points().map((p, i) => `${this.x(i)},${this.y(p.oee)}`).join(' '));

  readonly hovered = computed(() => {
    const index = this.hover();
    const point = index == null ? undefined : this.points()[index];
    if (index == null || !point) {
      return null;
    }
    return { x: this.x(index), y: this.y(point.oee), value: formatPercent(point.oee), time: formatTime(point.t) };
  });

  onMove(event: PointerEvent): void {
    const n = this.points().length;
    if (n === 0) {
      return;
    }
    const box = (event.currentTarget as HTMLElement).getBoundingClientRect();
    const ratio = Math.max(0, Math.min(1, (event.clientX - box.left) / box.width));
    this.hover.set(Math.round(ratio * (n - 1)));
  }
}
