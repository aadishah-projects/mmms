import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SettingsService, SystemSettings } from '../../service/settings.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.scss',
})
export class SettingsComponent implements OnInit {
  isLoaded = false;
  loadError = false;
  isSaving = false;

  activeTab: 'ai' | 'email' = 'ai';

  // AI Configuration
  aiProviderType = 'ANTHROPIC';
  aiBaseUrl = '';
  aiApiKey = '';
  aiHasApiKey = false;
  aiModel = '';
  aiMaxTokens = 2500;
  showAiApiKey = false;

  // AI Testing
  isTestingAi = false;
  testAiPrompt = 'Respond with a short confirmation message.';
  aiTestResult: string | null = null;
  aiTestError: string | null = null;

  // Email (SMTP) Configuration
  mailHost = '';
  mailPort = 587;
  mailUsername = '';
  mailPassword = '';
  mailHasPassword = false;
  mailAuth = true;
  mailStarttls = true;
  mailFrom = '';
  frontendUrl = 'http://localhost:4200';
  showMailPassword = false;

  // Email Testing
  isTestingEmail = false;
  testEmailAddress = '';
  emailTestResult: string | null = null;
  emailTestError: string | null = null;

  // Feedback notifications
  feedbackMessage: string | null = null;
  feedbackType: 'success' | 'error' = 'success';

  updatedAt: string | null = null;
  updatedBy: string | null = null;

  constructor(private settingsService: SettingsService) {}

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.loadError = false;
    this.isLoaded = false;
    this.settingsService.getSettings().subscribe({
      next: (response) => {
        const settings: SystemSettings = response.mainBody;
        if (settings.ai) {
          this.aiProviderType = settings.ai.providerType || 'ANTHROPIC';
          this.aiBaseUrl = settings.ai.baseUrl || '';
          this.aiHasApiKey = settings.ai.hasApiKey || false;
          this.aiApiKey = '';
          this.aiModel = settings.ai.model || '';
          this.aiMaxTokens = settings.ai.maxTokens || 2500;
        }

        if (settings.email) {
          this.mailHost = settings.email.host || '';
          this.mailPort = settings.email.port || 587;
          this.mailUsername = settings.email.username || '';
          this.mailHasPassword = settings.email.hasPassword || false;
          this.mailPassword = '';
          this.mailAuth = settings.email.auth !== undefined ? settings.email.auth : true;
          this.mailStarttls = settings.email.starttls !== undefined ? settings.email.starttls : true;
          this.mailFrom = settings.email.fromAddress || '';
          this.frontendUrl = settings.email.frontendUrl || 'http://localhost:4200';
        }

        this.updatedAt = settings.updatedAt || null;
        this.updatedBy = settings.updatedBy || null;
        this.isLoaded = true;
      },
      error: (err) => {
        this.loadError = true;
        this.isLoaded = true;
        console.error('Failed to load system settings', err);
      },
    });
  }

  saveSettings(): void {
    this.isSaving = true;

    const trimmedApiKey = (this.aiApiKey || '').trim();
    const trimmedMailPassword = (this.mailPassword || '').trim();

    const payload: Partial<SystemSettings> = {
      ai: {
        providerType: this.aiProviderType,
        baseUrl: this.aiBaseUrl,
        apiKey: trimmedApiKey.length > 0 ? trimmedApiKey : undefined,
        hasApiKey: this.aiHasApiKey,
        model: this.aiModel,
        maxTokens: this.aiMaxTokens,
      },
      email: {
        host: this.mailHost,
        port: this.mailPort,
        username: this.mailUsername,
        password: trimmedMailPassword.length > 0 ? trimmedMailPassword : undefined,
        hasPassword: this.mailHasPassword,
        auth: this.mailAuth,
        starttls: this.mailStarttls,
        fromAddress: this.mailFrom,
        frontendUrl: this.frontendUrl,
      },
    };

    this.settingsService.updateSettings(payload).subscribe({
      next: (response) => {
        const settings = response.mainBody;
        if (settings.ai) {
          this.aiHasApiKey = settings.ai.hasApiKey;
          this.aiApiKey = '';
        }
        if (settings.email) {
          this.mailHasPassword = settings.email.hasPassword;
          this.mailPassword = '';
        }
        this.updatedAt = settings.updatedAt || null;
        this.updatedBy = settings.updatedBy || null;

        this.feedbackMessage = 'System settings saved successfully!';
        this.feedbackType = 'success';
        this.isSaving = false;
        this.clearFeedback();
      },
      error: (err) => {
        this.feedbackMessage = err.error?.message || 'Failed to save system settings. Please try again.';
        this.feedbackType = 'error';
        this.isSaving = false;
        this.clearFeedback();
      },
    });
  }

  testAi(): void {
    // Save first, then test — so the user never has to manually click Save before testing
    this.isTestingAi = true;
    this.aiTestResult = null;
    this.aiTestError = null;

    const trimmedApiKey = (this.aiApiKey || '').trim();
    const payload = {
      ai: {
        providerType: this.aiProviderType,
        baseUrl: this.aiBaseUrl,
        apiKey: trimmedApiKey.length > 0 ? trimmedApiKey : undefined,
        hasApiKey: this.aiHasApiKey,
        model: this.aiModel,
        maxTokens: this.aiMaxTokens,
      },
      email: {
        host: this.mailHost,
        port: this.mailPort,
        username: this.mailUsername,
        password: undefined,
        hasPassword: this.mailHasPassword,
        auth: this.mailAuth,
        starttls: this.mailStarttls,
        fromAddress: this.mailFrom,
        frontendUrl: this.frontendUrl,
      },
    };

    this.settingsService.updateSettings(payload).subscribe({
      next: (saveResponse) => {
        const saved = saveResponse.mainBody;
        if (saved?.ai) {
          this.aiHasApiKey = saved.ai.hasApiKey;
          this.aiApiKey = '';
        }
        // Now run the actual test
        this.settingsService.testAi(this.testAiPrompt).subscribe({
          next: (response) => {
            this.aiTestResult = response.mainBody?.reply || 'Connected and verified successfully!';
            this.isTestingAi = false;
          },
          error: (err) => {
            this.aiTestError = err.error?.message || 'AI service test failed. Check your Base URL and API Key.';
            this.isTestingAi = false;
          },
        });
      },
      error: (err) => {
        this.aiTestError = 'Could not save settings before testing: ' + (err.error?.message || err.message);
        this.isTestingAi = false;
      },
    });
  }

  testEmail(): void {
    if (!this.testEmailAddress || !this.testEmailAddress.includes('@')) {
      this.emailTestError = 'Please enter a valid recipient email address for testing.';
      return;
    }

    this.isTestingEmail = true;
    this.emailTestResult = null;
    this.emailTestError = null;

    const trimmedMailPassword = (this.mailPassword || '').trim();
    const payload = {
      ai: {
        providerType: this.aiProviderType,
        baseUrl: this.aiBaseUrl,
        apiKey: undefined,
        hasApiKey: this.aiHasApiKey,
        model: this.aiModel,
        maxTokens: this.aiMaxTokens,
      },
      email: {
        host: this.mailHost,
        port: this.mailPort,
        username: this.mailUsername,
        password: trimmedMailPassword.length > 0 ? trimmedMailPassword : undefined,
        hasPassword: this.mailHasPassword,
        auth: this.mailAuth,
        starttls: this.mailStarttls,
        fromAddress: this.mailFrom,
        frontendUrl: this.frontendUrl,
      },
    };

    this.settingsService.updateSettings(payload).subscribe({
      next: (saveResponse) => {
        const saved = saveResponse.mainBody;
        if (saved?.email) {
          this.mailHasPassword = saved.email.hasPassword;
          this.mailPassword = '';
        }
        this.settingsService.testEmail(this.testEmailAddress.trim()).subscribe({
          next: (response) => {
            this.emailTestResult = response.message || 'Test email sent successfully!';
            this.isTestingEmail = false;
          },
          error: (err) => {
            this.emailTestError = err.error?.message || 'Failed to send test email. Check your SMTP host, port, username, and password.';
            this.isTestingEmail = false;
          },
        });
      },
      error: (err) => {
        this.emailTestError = 'Could not save settings before testing: ' + (err.error?.message || err.message);
        this.isTestingEmail = false;
      },
    });
  }

  setProviderDefaults(): void {
    if (this.aiProviderType === 'OPENAI_RESPONSES') {
      if (!this.aiBaseUrl || this.aiBaseUrl.includes('anthropic') || this.aiBaseUrl.includes('openai.com')) {
        this.aiBaseUrl = 'https://opencode.ai/zen/v1';
      }
      if (!this.aiModel || this.aiModel === 'mimo-v2.5-pro' || this.aiModel === 'gpt-4o-mini') {
        this.aiModel = 'muse-spark-1.2-contributor-free';
      }
    } else if (this.aiProviderType === 'OPENAI_COMPATIBLE') {
      if (!this.aiBaseUrl || this.aiBaseUrl.includes('anthropic')) {
        this.aiBaseUrl = 'https://api.openai.com';
      }
      if (!this.aiModel || this.aiModel === 'mimo-v2.5-pro') {
        this.aiModel = 'gpt-4o-mini';
      }
    } else {
      if (!this.aiBaseUrl || this.aiBaseUrl.includes('openai')) {
        this.aiBaseUrl = 'https://api.xiaomimimo.com/anthropic';
      }
      if (!this.aiModel || this.aiModel === 'gpt-4o-mini') {
        this.aiModel = 'mimo-v2.5-pro';
      }
    }
  }

  private clearFeedback(): void {
    setTimeout(() => {
      this.feedbackMessage = null;
    }, 4000);
  }
}
