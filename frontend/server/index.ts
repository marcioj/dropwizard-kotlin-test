import type { Handler } from "vite-plugin-mix";
import { createHTTPHandler } from "@trpc/server/adapters/standalone";
import { z } from "zod";
import { request } from "../src/utils";
import { publicProcedure, router } from "./trpc";
import { Post, PostValidator as PostSchema } from "../src/types";
import { TRPC_URL_PREFIX } from "../src/client";

const appRouter = router({
  post: {
    list: publicProcedure.output(z.array(PostSchema)).query(async () => {
      return await request<Post[]>("/api/posts");
    }),
    find: publicProcedure
      .input(z.string())
      .output(PostSchema)
      .query(async ({ input }) => {
        return await request<Post>(`/api/posts/${input}`);
      }),
    create: publicProcedure
      .input(
        z.object({
          title: z.string(),
          content: z.string(),
        })
      )
      .output(PostSchema)
      .mutation(async ({ input }) => {
        return await request<Post>(`/api/posts`, {
          method: "post",
          body: JSON.stringify(input),
        });
      }),
    update: publicProcedure
      .input(PostSchema)
      .output(PostSchema)
      .mutation(async ({ input }) => {
        return await request<Post>(`/api/posts/${input.id}`, {
          method: "put",
          body: JSON.stringify(input),
        });
      }),
    destroy: publicProcedure.input(z.string()).mutation(async ({ input }) => {
      return await request<boolean>(`/api/posts/${input}`, {
        method: "delete",
      });
    }),
  },
});

export type AppRouter = typeof appRouter;

export const trpcHandler = createHTTPHandler({
  router: appRouter,
});

export const handler: Handler = (req, res, next) => {
  if (req.url.startsWith(TRPC_URL_PREFIX + "/")) {
    req.url = req.url.replace(TRPC_URL_PREFIX, "");
    trpcHandler(req, res);
  } else {
    next();
  }
};
