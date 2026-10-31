/** Mise en forme des nombres à la française (virgule décimale, espace pour les milliers). */

const percentFormat = new Intl.NumberFormat('fr-FR', { style: 'percent', minimumFractionDigits: 1, maximumFractionDigits: 1 });
const integerFormat = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 });
const decimalFormat = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const timeFormat = new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });

export const formatPercent = (ratio: number): string => percentFormat.format(ratio);
export const formatInteger = (value: number): string => integerFormat.format(value);
export const formatDecimal = (value: number): string => decimalFormat.format(value);
export const formatTime = (epochMillis: number | null): string =>
  epochMillis == null ? '—' : timeFormat.format(new Date(epochMillis));
