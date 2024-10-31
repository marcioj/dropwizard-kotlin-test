import { createTRPCClient, httpBatchLink } from '@trpc/client';
import type { AppRouter } from '../server';

// Use a prefix for trpc http calls so that is easier to handle them inside the server handler
export const TRPC_URL_PREFIX = '/trpc'

export const trpc = createTRPCClient<AppRouter>({
  links: [
    httpBatchLink({
      url: TRPC_URL_PREFIX
    }),
  ],
});
