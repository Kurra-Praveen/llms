/**
 * Normalizes a TanStack Query result into an explicit state machine so
 * screens render loading / empty / error / ready instead of silently
 * showing stale or blank content.
 *
 * Usage:
 *   const state = useQueryState(query);
 *   if (!state.isReady) {
 *     return <ErrorState variant={state.status} onRetry={state.refetch} />;
 *   }
 *   // state.data is guaranteed non-null here
 */

export type QueryStatus = 'loading' | 'empty' | 'error' | 'ready';

export interface QueryStateBase {
  status: QueryStatus;
  error: unknown;
  isRefetching: boolean;
  refetch: () => void;
}

export interface QueryLoading extends QueryStateBase {
  status: 'loading';
  isReady: false;
  data: null;
}

export interface QueryEmpty extends QueryStateBase {
  status: 'empty';
  isReady: false;
  data: null;
}

export interface QueryError extends QueryStateBase {
  status: 'error';
  isReady: false;
  data: unknown;
}

export interface QueryReady<T> extends QueryStateBase {
  status: 'ready';
  isReady: true;
  data: T;
}

export type QueryState<T> = QueryLoading | QueryEmpty | QueryError | QueryReady<T>;

export function useQueryState<T>(query: {
  data?: T | null;
  isLoading: boolean;
  isRefetching: boolean;
  isError: boolean;
  error: unknown;
  refetch: () => void;
}): QueryState<T> {
  const { isLoading, isError, error, refetch, isRefetching, data } = query;

  if (isLoading) {
    return { status: 'loading', isReady: false, data: null, error, isRefetching, refetch };
  }
  if (isError) {
    return { status: 'error', isReady: false, data: data ?? null, error, isRefetching, refetch };
  }
  if (data == null) {
    return { status: 'empty', isReady: false, data: null, error, isRefetching, refetch };
  }

  return { status: 'ready', isReady: true, data, error, isRefetching, refetch };
}