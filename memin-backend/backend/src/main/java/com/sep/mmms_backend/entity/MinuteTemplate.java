package com.sep.mmms_backend.entity;

import com.sep.mmms_backend.enums.MinuteLanguage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A reusable, named minute layout owned by one committee.
 *
 * Committee.minuteTemplateHtml is retained as the active rendered template
 * for compatibility with existing meetings and older database records. This
 * entity provides the template library requested by the UI.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "minute_templates", uniqueConstraints = {
        @UniqueConstraint(name = "uk_minute_template_committee_name", columnNames = {"committee_id", "template_name"})
})
public class MinuteTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Integer id;

    @Column(name = "template_name", nullable = false, length = 160)
    private String name;

    @Column(name = "template_html", nullable = false, columnDefinition = "TEXT")
    private String minuteTemplateHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_language", length = 30)
    private MinuteLanguage minuteLanguage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "committee_id", nullable = false)
    private Committee committee;

    @Column(name = "created_by")
    private String createdBy;
}
