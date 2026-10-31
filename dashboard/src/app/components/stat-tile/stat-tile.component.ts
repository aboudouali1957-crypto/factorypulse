import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Une tuile de chiffre clé : un libellé, une valeur, et une précision facultative. */
@Component({
  selector: 'fp-stat-tile',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="label">{{ label() }}</span>
    <span class="value num">{{ value() }}</span>
    @if (detail()) {
      <span class="detail">{{ detail() }}</span>
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: 4px; padding: 16px; background: var(--surface-1);
            border: 1px solid var(--border); border-radius: var(--radius); min-width: 0; }
    .label { font-size: 13px; color: var(--text-secondary); }
    .value { font-size: 26px; font-weight: 600; line-height: 1.1; }
    .detail { font-size: 12px; color: var(--text-muted); }
  `,
})
export class StatTileComponent {
  readonly label = input.required<string>();
  readonly value = input.required<string>();
  readonly detail = input<string>('');
}
