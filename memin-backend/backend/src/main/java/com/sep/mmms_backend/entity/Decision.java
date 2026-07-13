package com.sep.mmms_backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "decisions")
@EntityListeners(AuditingEntityListener.class)
public class Decision {
    @Id
    @Column(name="decision_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer decisionId;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @ManyToOne
    @JoinColumn(name="meeting_id", referencedColumnName = "meeting_id", nullable=false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Meeting meeting;

    @Column(name =  "decision")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String decision;


    @Column(name = "decision_created_by", updatable = false, nullable = false)
    @CreatedBy
    private String createdBy;

    @Column(name = "decision_created_date", updatable = false, nullable = false)
    @CreatedDate
    private LocalDate createdDate;

    @Column(name = "decision_modified_by",  nullable = false)
    @LastModifiedBy
    private String modifiedBy;

    @Column(name = "decision_modified_date", nullable = false)
    @LastModifiedDate
    private LocalDate modifiedDate;


    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
    }


    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Decision that = (Decision) o;
        return Objects.equals(uuid, that.uuid);
    }
}
