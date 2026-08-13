import { Observable } from 'rxjs';
import { timeout, catchError } from 'rxjs/operators';

export interface ColdStartConfig {
  firstTimeoutMs?: number;   // Default: 15,000ms (15s)
  retryTimeoutMs?: number;   // Default: 120,000ms (120s)
  onWakingUp?: () => void;   // Called when 1st attempt times out & server is waking up
}

/**
 * Executes an HTTP request with cold-start awareness.
 * Attempt 1: 15s timeout.
 * If 1st attempt times out, calls onWakingUp() callback and retries ONCE with up to 120s timeout.
 * Max automatic requests: 2.
 */
export function executeWithColdStartRetry<T>(
  requestFactory: () => Observable<T>,
  config?: ColdStartConfig
): Observable<T> {
  const firstTimeout = config?.firstTimeoutMs ?? 15000;
  const retryTimeout = config?.retryTimeoutMs ?? 120000;

  return requestFactory().pipe(
    timeout(firstTimeout),
    catchError(err => {
      // First attempt failed or timed out — trigger waking up callback
      if (config?.onWakingUp) {
        config.onWakingUp();
      }
      // Attempt 2 (Retry 1): Up to 120s timeout
      return requestFactory().pipe(
        timeout(retryTimeout)
      );
    })
  );
}
