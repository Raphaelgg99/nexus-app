import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, timeout } from 'rxjs';

import { resolveApiUrl } from './api-url';

export interface ChatBotStatus {
  id: number;
  active: boolean;
}

interface ChatBotStatusResponse {
  id: number;
  active?: boolean;
  isActive?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class Chatbot {
  private readonly endpoint = resolveApiUrl('/api/v1/chatbot');

  constructor(private readonly http: HttpClient) {}

  getStatus(): Observable<ChatBotStatus> {
    return this.http.get<ChatBotStatusResponse>(this.endpoint).pipe(
      timeout(8000),
      map((chatbot) => this.normalizeStatus(chatbot)),
    );
  }

  toggleStatus(): Observable<ChatBotStatus> {
    return this.http.patch<ChatBotStatusResponse>(`${this.endpoint}/toggle`, null).pipe(
      timeout(8000),
      map((chatbot) => this.normalizeStatus(chatbot)),
    );
  }

  private normalizeStatus(chatbot: ChatBotStatusResponse): ChatBotStatus {
    return {
      id: chatbot.id,
      active: chatbot.active ?? chatbot.isActive ?? false,
    };
  }
}
