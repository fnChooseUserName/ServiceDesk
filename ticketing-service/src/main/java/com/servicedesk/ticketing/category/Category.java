package com.servicedesk.ticketing.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "category", schema = "ticketing")
public class Category {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "default_sla_hours", nullable = false)
    private int defaultSlaHours;

    protected Category() {
        // required by JPA
    }

    public Category(UUID id, String name, String description, int defaultSlaHours) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.defaultSlaHours = defaultSlaHours;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDefaultSlaHours() {
        return defaultSlaHours;
    }
}
