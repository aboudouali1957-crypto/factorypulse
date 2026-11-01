/**
 * Adresse WebSocket sur le même hôte que la page.
 * En local, un proxy (ng serve ou nginx) redirige ensuite vers le bon service :
 * "/ws" → kpi-service, "/ws-alerts" → alert-service.
 */
export function sameHostWebSocketUrl(path: string): string {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${location.host}${path}`;
}
