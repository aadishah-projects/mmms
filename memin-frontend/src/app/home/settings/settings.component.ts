import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  AiConfiguration,
  AiConfigurationPayload,
  AiConfigurationService,
  AiProviderType,
} from '../../service/ai-configuration.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  configuration: AiConfiguration | null = null;
  isLoaded = false;
  isSaving = false;
  isTesting = false;
  isClearing = false;
  loadError = false;
  feedbackMessage: string | null = null;
  feedbackType: 'success' | 'error' = 'success';
  validationMessage: string | null = null;

  enabled = true;
  provider: AiProviderType = 'ANTHROPIC_COMPATIBLE';
  baseUrl = '';
  apiKey = '';
  model = 'mimo-v2.5-pro';
  maxTokens = 2500;
  additionalInstructions = '';

  constructor(private readonly aiConfigurationService: AiConfigurationService) {}

  ngOnInit(): void {
    this.loadConfiguration();
  }

  loadConfiguration(): void {
    this.isLoaded = false;
    this.loadError = false;
    this.aiConfigurationService.getConfiguration().subscribe({
      next: (response) => {
        this.configuration = response.mainBody;
        this.applyConfiguration(this.configuration);
        this.isLoaded = true;
      },
      error: (error) => {
        this.loadError = true;
        this.isLoaded = true;
        console.error('Failed to load AI configuration', error);
      },
    });
  }

  save(): void {
    this.validationMessage = this.validateForm();
    if (this.validationMessage || this.isSaving) {
      return;
    }

    this.isSaving = true;
    this.aiConfigurationService.updateConfiguration(this.payload()).subscribe({
      next: (response) => {
        this.configuration = response.mainBody;
        this.applyConfiguration(this.configuration, true);
        this.showFeedback('AI configuration saved successfully', 'success');
        this.isSaving = false;
      },
      error: (error) => {
        this.showFeedback(error.error?.message || 'Could not save AI configuration', 'error');
        this.isSaving = false;
      },
    });
  }

  testConnection(): void {
    this.validationMessage = this.validateForm(true);
    if (this.validationMessage || this.isTesting) {
      return;
    }

    this.isTesting = true;
    this.aiConfigurationService.testConnection(this.payload()).subscribe({
      next: (response) => {
        const result = response.mainBody;
        this.showFeedback(result.message, result.success ? 'success' : 'error');
        this.isTesting = false;
      },
      error: (error) => {
        this.showFeedback(error.error?.message || 'Could not test the AI connection', 'error');
        this.isTesting = false;
      },
    });
  }

  clearSavedConfiguration(): void {
    if (this.isClearing || !window.confirm('Clear the saved AI configuration and use environment defaults?')) {
      return;
    }

    this.isClearing = true;
    this.aiConfigurationService.clearConfiguration().subscribe({
      next: (response) => {
        this.configuration = response.mainBody;
        this.applyConfiguration(this.configuration, true);
        this.showFeedback('Saved configuration cleared. Environment defaults are active if available.', 'success');
        this.isClearing = false;
      },
      error: (error) => {
        this.showFeedback(error.error?.message || 'Could not clear the saved AI configuration', 'error');
        this.isClearing = false;
      },
    });
  }

  get statusText(): string {
    if (!this.configuration) {
      return 'Loading';
    }
    if (!this.configuration.enabled) {
      return 'Disabled';
    }
    if (!this.configuration.apiKeyConfigured || !this.configuration.baseUrl || !this.configuration.model) {
      return 'Not configured';
    }
    return 'Ready';
  }

  get sourceText(): string {
    if (!this.configuration) {
      return '';
    }
    if (this.configuration.source === 'DATABASE') {
      return 'Saved in the application database';
    }
    if (this.configuration.source === 'ENVIRONMENT') {
      return 'Using deployment environment defaults';
    }
    return 'No provider configuration is active';
  }

  private applyConfiguration(configuration: AiConfiguration, clearApiKey = false): void {
    this.enabled = configuration.enabled;
    this.provider = configuration.provider || 'ANTHROPIC_COMPATIBLE';
    this.baseUrl = configuration.baseUrl || '';
    this.model = configuration.model || 'mimo-v2.5-pro';
    this.maxTokens = configuration.maxTokens || 2500;
    this.additionalInstructions = configuration.additionalInstructions || '';
    if (clearApiKey || !configuration.apiKeyConfigured) {
      this.apiKey = '';
    }
    this.validationMessage = null;
  }

  private payload(): AiConfigurationPayload {
    return {
      enabled: this.enabled,
      provider: this.provider,
      baseUrl: this.baseUrl.trim(),
      apiKey: this.apiKey.trim(),
      model: this.model.trim(),
      maxTokens: Number(this.maxTokens),
      additionalInstructions: this.additionalInstructions.trim(),
    };
  }

  private validateForm(forTest = false): string | null {
    if (!this.enabled && !forTest) {
      return null;
    }
    if (!this.baseUrl.trim()) {
      return 'Enter the AI provider base URL.';
    }
    if (!/^https?:\/\//i.test(this.baseUrl.trim())) {
      return 'The base URL must start with http:// or https://.';
    }
    if (!this.model.trim()) {
      return 'Enter a model name.';
    }
    if (!Number.isInteger(Number(this.maxTokens)) || Number(this.maxTokens) < 500 || Number(this.maxTokens) > 100000) {
      return 'Maximum output tokens must be between 500 and 100000.';
    }
    if (!this.apiKey.trim() && !this.configuration?.apiKeyConfigured) {
      return 'Enter an API key. It will be stored securely by the backend.';
    }
    return null;
  }

  private showFeedback(message: string, type: 'success' | 'error'): void {
    this.feedbackMessage = message;
    this.feedbackType = type;
    window.setTimeout(() => {
      this.feedbackMessage = null;
    }, 4500);
  }
}
