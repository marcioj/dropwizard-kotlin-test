export type Post = {
  id: number;
  title: string;
  content: string;
};

export type ValidationErrorEntry = {
  field: string;
  message: string;
};

export class APIValidationError extends Error {
  code: string;
  message: string;
  errors: ValidationErrorEntry[];

  constructor(message: string, code: string, errors: ValidationErrorEntry[]) {
    super()
    this.message = message
    this.code = code
    this.errors = errors
  }
};
