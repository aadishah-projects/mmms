package com.sep.mmms_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(name = "id")
    private Integer id = 1;

    // AI Settings
    @Column(name = "ai_provider_type", length = 50)
    private String aiProviderType; // "ANTHROPIC", "OPENAI_COMPATIBLE", or "OPENAI_RESPONSES"

    @Column(name = "ai_base_url", length = 500)
    private String aiBaseUrl;

    @Column(name = "ai_api_key", length = 500)
    private String aiApiKey;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Column(name = "ai_max_tokens")
    private Integer aiMaxTokens;

    // Email (SMTP) Settings
    @Column(name = "mail_host", length = 255)
    private String mailHost;

    @Column(name = "mail_port")
    private Integer mailPort;

    @Column(name = "mail_username", length = 255)
    private String mailUsername;

    @Column(name = "mail_password", length = 255)
    private String mailPassword;

    @Column(name = "mail_auth")
    private Boolean mailAuth;

    @Column(name = "mail_starttls")
    private Boolean mailStarttls;

    @Column(name = "mail_from", length = 255)
    private String mailFrom;

    @Column(name = "frontend_url", length = 255)
    private String frontendUrl;

    // Audit
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    public void prePersistOrUpdate() {
        if (this.id == null) {
            this.id = 1;
        }
        this.updatedAt = LocalDateTime.now();
    }
}
