package com.codesage.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base entity providing UUID primary key and audit timestamps.
 *
 * <p>Architecture Spec mandates:
 * <ul>
 *   <li>UUID primary keys</li>
 *   <li>TIMESTAMPTZ (mapped as {@link Instant}) everywhere</li>
 *   <li>No Hibernate schema generation — Flyway manages DDL</li>
 * </ul>
 *
 * <p>All domain entities must extend this class.
 *
 * <p>{@link AuditingEntityListener} is activated by {@code @EnableJpaAuditing}
 * in {@link com.codesage.CodeSageApplication}.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant updatedAt;
}
