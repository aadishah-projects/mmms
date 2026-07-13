package com.sep.mmms_backend.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="meetings")
@EntityListeners(AuditingEntityListener.class)
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Integer id;

    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private String uuid;

    @Column(name = "meeting_title")
    private String title;

    @Column(name = "meeting_held_date")
    private LocalDate heldDate;

    @Column(name = "meeting_held_time")
    private LocalTime heldTime;

    @Column(name = "meeting_held_place")
    private String heldPlace;

    @Column(name = "created_by", updatable = false, nullable = false)
    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    private LocalDate createdDate;

    @LastModifiedDate
    @Column(name = "updated_date", nullable = false)
    private LocalDate updatedDate;

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "committee_id", referencedColumnName = "committee_id")
    Committee committee;


    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "meeting_attendees",
            inverseJoinColumns = {
                    @JoinColumn(name = "member_id", referencedColumnName = "member_id"),
            },

            joinColumns = {
                    @JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id"),
            }
    )
    public List<Member> attendees = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "meeting_invitees",
            inverseJoinColumns = {
                    @JoinColumn(name = "member_id", referencedColumnName = "member_id"),
            },

            joinColumns = {
                    @JoinColumn(name = "meeting_id", referencedColumnName = "meeting_id"),
            }
    )
    public List<Member> invitees = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Decision> decisions = new ArrayList<>();

    @OneToMany(mappedBy = "meeting", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Agenda> agendas = new ArrayList<>();

    public void addDecision(Decision decision) {
        decision.setMeeting(this);
        this.decisions.add(decision);
    }


    public void addAllDecisions(List<Decision> decisions) {
        decisions.forEach(decision -> decision.setMeeting(this));
        this.decisions.addAll(decisions);
    }

    public void addAgenda(Agenda agenda) {
        agenda.setMeeting(this);
        this.agendas.add(agenda);
    }


    public void addAllAgendas(List<Agenda> agendas) {
        agendas.forEach(agenda -> agenda.setMeeting(this));
        this.agendas.addAll(agendas);
    }

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

        Meeting that = (Meeting) o;
        return Objects.equals(uuid, that.uuid);
    }
    //The issue with not using uuid is that:
    /*
        The issue with the above implementation is that for an unsaved entity 'm'

        m.equals(m) returns false because m.id is 0

        now after saving the entity

        m.equals(m) returns true because m.id is > 0 and are equal

-------------------------------------------------------------------------------------------

        Furthermore, how should hashcode be implemented?

        The equality is checked by 'id'. So if two objets m1 and m2 are deemed equal by equal(), then they must have same hashcode.

        Therefore, hashcode must be generated based on id.

        Howevever, the id changes when an unsaved entity is saved.

        Suppose I have an unsaved object 'm'. The id of this object is '0' and has a hashcode say 0000.

        After save operation, the id of this object changes to some other value say '2', and the hashcode for this object now becomes 2222. Here the hashcode has changed due to persist operation.

        If an object's hashcode changes while it's in HashSet/HashMap or any other data structures that uses Hash, it breaks

----------------------------------------------------------------------------------------------

        Another subtle bug, hibernate often uses proxy objects for lazy loading. A simple 'instanceof' check can sometimes fail when comparing an entity with its proxy.
     */
}
