import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { resolveApiUrl } from './api-url';

export type UserRole = 'ADMIN' | 'USER';

export interface UserResponse {
  id: number;
  userName: string;
  role: UserRole;
}

export interface UserCreateRequest {
  userName: string;
  role: UserRole;
  password: string;
}

export interface UserUpdateRequest {
  userName: string;
  role: UserRole;
  password?: string;
}

@Injectable({
  providedIn: 'root',
})
export class User {
  private readonly endpoint = resolveApiUrl('/api/v1/users');

  constructor(private readonly http: HttpClient) {}

  findAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.endpoint);
  }

  create(request: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.endpoint, request);
  }

  update(userId: number, request: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.endpoint}/${userId}`, request);
  }

  delete(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${userId}`);
  }
}
