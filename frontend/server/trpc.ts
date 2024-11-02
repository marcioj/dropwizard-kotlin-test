import { initTRPC } from "@trpc/server";
import { APIError, APIValidationError, ValidationErrorData } from "../src/types";
import { getStatusKeyFromCode, TRPCErrorShape } from "@trpc/server/unstable-core-do-not-import";
import { ZodError } from "zod";

interface ValidationErrorShape extends TRPCErrorShape<ValidationErrorData> {
}

const t = initTRPC.create({
  errorFormatter(opts) {
    const { shape, error } = opts;

    if (error.cause instanceof APIError) {
      const httpStatus = Number(error.cause.code)
      const errorResponse = {
        ...shape,
        data: {
          ...shape.data,
          code: getStatusKeyFromCode(httpStatus),
          httpStatus,
        },
      };

      if (error.cause instanceof APIValidationError) {
        (errorResponse as ValidationErrorShape).data.errors = error.cause.errors
      }

      return errorResponse
    } else if (error.cause instanceof ZodError) {
      return {
        ...shape,
        data: {
          ...shape.data,
          code: "BAD_REQUEST",
          httpStatus: 400,
          errors: error.cause.errors
        },
      }
    }

    return shape
  },
});

export const router = t.router;
export const publicProcedure = t.procedure;
