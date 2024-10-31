import { initTRPC } from "@trpc/server";

const t = initTRPC.create({
  errorFormatter(opts) {
    const { shape, error } = opts;

    return error.cause

    return {
      ...shape,
      data: {
        ...shape.data,
      },
    };
  },
});

export const router = t.router;
export const publicProcedure = t.procedure;
