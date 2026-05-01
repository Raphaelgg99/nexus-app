import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, throwError, timeout } from 'rxjs';

import { resolveApiUrl } from './api-url';

export interface GeneratedPrototype {
  imageUrl: string;
  [key: string]: unknown;
}

export interface ImprovedPromptResponse {
  prompt: string;
}

@Injectable({
  providedIn: 'root',
})
export class Prototype {
  private readonly endpoint = resolveApiUrl('/api/prototype/generate');
  private readonly improvePromptEndpoint = resolveApiUrl('/api/melhorar-prompt');

  constructor(private readonly http: HttpClient) {}

  generate(files: File[], prompt: string): Observable<GeneratedPrototype> {
    const formData = new FormData();

    for (const file of files) {
      formData.append('file', file);
    }

    formData.append('prompt', prompt);

    return this.http.post<GeneratedPrototype>(this.endpoint, formData).pipe(
      timeout(60000),
      catchError((error) => {
        if (error?.name === 'TimeoutError') {
          return throwError(
            () => new Error('A requisicao demorou demais para responder.')
          );
        }

        return throwError(() => error);
      })
    );
  }

  improvePrompt(prompt: string): Observable<ImprovedPromptResponse> {
    return this.http
      .post<ImprovedPromptResponse>(this.improvePromptEndpoint, { prompt })
      .pipe(
        timeout(30000),
        catchError((error) => {
          if (error?.name === 'TimeoutError') {
            return throwError(
              () => new Error('O aperfeicoamento do prompt demorou demais para responder.')
            );
          }

          return throwError(() => error);
        })
      );
  }
}
