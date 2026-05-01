import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, switchMap } from 'rxjs';

import { resolveApiUrl } from './api-url';

export interface FolderResponse {
  id: number;
  name: string;
}

export interface FolderRequest {
  name: string;
}

export interface MockupRequest {
  image: string;
  name?: string;
  type?: string;
  description?: string;
  sku?: string;
  available?: boolean;
  stockQuantity?: number;
}

export interface MockupResponse {
  id: number;
  image: string;
  name: string;
  type: string;
  description: string | null;
  sku: string | null;
  available: boolean;
  stockQuantity: number;
  folderId: number;
}

@Injectable({
  providedIn: 'root',
})
export class FolderMockup {
  private readonly foldersEndpoint = resolveApiUrl('/api/v1/folders');

  constructor(private readonly http: HttpClient) {}

  findAllFolders(): Observable<FolderResponse[]> {
    return this.http.get<FolderResponse[]>(this.foldersEndpoint);
  }

  createFolder(request: FolderRequest): Observable<FolderResponse> {
    return this.http.post<FolderResponse>(this.foldersEndpoint, request);
  }

  updateFolder(folderId: number, request: FolderRequest): Observable<FolderResponse> {
    return this.http.put<FolderResponse>(
      resolveApiUrl(`/api/v1/folders/${folderId}`),
      request
    );
  }

  deleteFolder(folderId: number): Observable<void> {
    return this.http.delete<void>(resolveApiUrl(`/api/v1/folders/${folderId}`));
  }

  findMockupsByFolderId(folderId: number): Observable<MockupResponse[]> {
    return this.http.get<MockupResponse[]>(
      resolveApiUrl(`/api/v1/folders/${folderId}/mockups`)
    );
  }

  ensureFolder(name: string): Observable<FolderResponse> {
    return this.findAllFolders().pipe(
      switchMap((folders) => {
        const normalizedName = this.normalizeName(name);
        const existingFolder = folders.find(
          (folder) => this.normalizeName(folder.name) === normalizedName
        );

        if (existingFolder) {
          return of(existingFolder);
        }

        return this.createFolder({ name });
      })
    );
  }

  createMockup(folderId: number, request: MockupRequest): Observable<MockupResponse> {
    return this.http.post<MockupResponse>(
      resolveApiUrl(`/api/v1/folders/${folderId}/mockups`),
      request
    );
  }

  updateMockup(
    folderId: number,
    mockupId: number,
    request: MockupRequest
  ): Observable<MockupResponse> {
    return this.http.put<MockupResponse>(
      resolveApiUrl(`/api/v1/folders/${folderId}/mockups/${mockupId}`),
      request
    );
  }

  deleteMockup(folderId: number, mockupId: number): Observable<void> {
    return this.http.delete<void>(
      resolveApiUrl(`/api/v1/folders/${folderId}/mockups/${mockupId}`)
    );
  }

  private normalizeName(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
