import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BACKEND_URL } from '../../global_constants';
import { Response } from '../response/response';

export interface AiSettings {
  providerType: string;
  baseUrl: string;
  apiKey?: string;
  hasApiKey: boolean;
  model: string;
  maxTokens: number;
}

export interface EmailSettings {
  host: string;
  port: number;
  username: string;
  password?: string;
  hasPassword: boolean;
  auth: boolean;
  starttls: boolean;
  fromAddress: string;
  frontendUrl: string;
}

export interface SystemSettings {
  ai: AiSettings;
  email: EmailSettings;
  updatedAt?: string;
  updatedBy?: string;
}

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private apiUrl = `${BACKEND_URL}/api/settings`;

  constructor(private http: HttpClient) {}

  getSettings(): Observable<Response<SystemSettings>> {
    return this.http.get<Response<SystemSettings>>(this.apiUrl, {
      withCredentials: true,
    });
  }

  updateSettings(settings: Partial<SystemSettings>): Observable<Response<SystemSettings>> {
    return this.http.put<Response<SystemSettings>>(this.apiUrl, settings, {
      withCredentials: true,
    });
  }

  testEmail(toEmail: string): Observable<Response<string>> {
    return this.http.post<Response<string>>(
      `${this.apiUrl}/test-email`,
      { toEmail },
      { withCredentials: true }
    );
  }

  testAi(prompt?: string): Observable<Response<{ reply: string }>> {
    return this.http.post<Response<{ reply: string }>>(
      `${this.apiUrl}/test-ai`,
      prompt ? { prompt } : {},
      { withCredentials: true }
    );
  }
}
