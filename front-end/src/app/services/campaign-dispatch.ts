import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { catchError, Observable, throwError, timeout } from 'rxjs';

import { resolveApiUrl } from './api-url';

export interface CampaignDispatchResponse {
  campaignName: string;
  totalRows: number;
  validLeads: number;
  invalidLeads: number;
  duplicateLeads: number;
  delayBetweenMessages: number;
  sentToN8n: boolean;
  errors: string[];
}

@Injectable({
  providedIn: 'root',
})
export class CampaignDispatch {
  private readonly endpoint = resolveApiUrl('/api/v1/campaigns/dispatch');

  constructor(private readonly http: HttpClient) {}

  dispatch(
    campaignName: string,
    message: string,
    delayBetweenMessages: number,
    leadsFile: File
  ): Observable<CampaignDispatchResponse> {
    const formData = new FormData();
    formData.append('campaignName', campaignName);
    formData.append('message', message);
    formData.append('delayBetweenMessages', String(delayBetweenMessages));
    formData.append('leadsFile', leadsFile);

    return this.http.post<CampaignDispatchResponse>(this.endpoint, formData).pipe(
      timeout(45000),
      catchError((error) => {
        if (error?.name === 'TimeoutError') {
          return throwError(() => new Error('O envio da campanha demorou demais para responder.'));
        }

        return throwError(() => error);
      })
    );
  }
}
