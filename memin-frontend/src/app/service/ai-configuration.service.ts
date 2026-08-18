import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BACKEND_URL } from '../../global_constants';
import { Response } from '../response/response';

export type AiProviderType = 'ANTHROPIC_COMPATIBLE' | 'OPENAI_COMPATIBLE';

export interface AiConfiguration {
  enabled: boolean;
  provider: AiProviderType;
  baseUrl: string | null;
  model: string | null;
  maxTokens: number;
  additionalInstructions: string | null;
  apiKeyConfigured: boolean;
  maskedApiKey: string | null;
  source: 'DATABASE' | 'ENVIRONMENT' | 'NONE';
}

export interface AiConfigurationPayload {
  enabled: boolean;
  provider: AiProviderType;
  baseUrl: string;
  apiKey: string;
  model: string;
  maxTokens: number;
  additionalInstructions: string;
}

export interface AiConnectionTestResult {
  success: boolean;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AiConfigurationService {
  private readonly endpoint = `${BACKEND_URL}/api/settings/ai`;

  constructor(private readonly httpClient: HttpClient) {}

  getConfiguration(): Observable<Response<AiConfiguration>> {
    return this.httpClient.get<Response<AiConfiguration>>(this.endpoint, {
      withCredentials: true,
    });
  }

  updateConfiguration(payload: AiConfigurationPayload): Observable<Response<AiConfiguration>> {
    return this.httpClient.put<Response<AiConfiguration>>(this.endpoint, payload, {
      withCredentials: true,
    });
  }

  testConnection(payload: AiConfigurationPayload): Observable<Response<AiConnectionTestResult>> {
    return this.httpClient.post<Response<AiConnectionTestResult>>(`${this.endpoint}/test`, payload, {
      withCredentials: true,
    });
  }

  clearConfiguration(): Observable<Response<AiConfiguration>> {
    return this.httpClient.delete<Response<AiConfiguration>>(this.endpoint, {
      withCredentials: true,
    });
  }
}
