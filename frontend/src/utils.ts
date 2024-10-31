import { APIValidationError } from './types';

export function request<T>(url: string, config: RequestInit = {}) {
  return fetch("http://localhost:8080" + url, {
    headers: { "Content-Type": "application/json" },
    ...config,
  }).then((res) => {
    return res.json().then((result) => {
      if (res.ok) {
        return result as T;
      } else if (res.status === 422) {
        return Promise.reject(new APIValidationError(result.message, result.code, result.errors))
      }
      return Promise.reject(result)
    });
  });
}
