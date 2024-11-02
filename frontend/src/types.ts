import { DefaultErrorData } from "@trpc/server/unstable-core-do-not-import";
import { z } from "zod";

export const PostValidator = z.object({
  id: z.number(),
  title: z.string(),
  content: z.string(),
})

export type Post = z.infer<typeof PostValidator>

export type ValidationErrorEntry = {
  field: string;
  message: string;
};

export class APIError extends Error {
  code: number;

  constructor(message: string, code: number) {
    super(message)
    this.code = code
  }
};

export class APIValidationError extends APIError {
  errors: ValidationErrorEntry[];

  constructor(message: string, code: number, errors: ValidationErrorEntry[]) {
    super(message, code)
    this.errors = errors
  }
};

export type ValidationErrorData = {
  errors: ValidationErrorEntry[]
} & DefaultErrorData
