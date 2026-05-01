const LOCAL_API_ORIGIN = 'http://localhost:8080';

export function resolveApiUrl(path: string): string {
  if (
    typeof window !== 'undefined' &&
    window.location.hostname === 'localhost' &&
    window.location.port === '4200'
  ) {
    return `${LOCAL_API_ORIGIN}${path}`;
  }

  return path;
}

export function isBackendRequestUrl(url: string): boolean {
  return url.startsWith('/api') || url.startsWith(`${LOCAL_API_ORIGIN}/api`);
}
