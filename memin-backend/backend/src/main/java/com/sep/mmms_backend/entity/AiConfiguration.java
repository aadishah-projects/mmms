package com.sep.mmms_backend.entity;

import com.sep.mmms_backend.enums.AiProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * The application-wide AI configuration. There is intentionally one row: the
 * provider credential is shared by the server-side AI minute service.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ai_configurations")
public class AiConfiguration {
    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "configuration_id", nullable = false)
    private Integer id = SINGLETON_ID;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private AiProviderType provider = AiProviderType.ANTHROPIC_COMPATIBLE;

    @Column(name = "base_url", length = 1000)
    private String baseUrl;

    /** Encrypted with the server's APP_ENCRYPTION_KEY. */
    @Column(name = "encrypted_api_key", length = 4096)
    private String encryptedApiKey;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "max_tokens")
    private Integer maxTokens = 2500;

    @Column(name = "additional_instructions", columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_date")
    private LocalDate updatedDate;
}
