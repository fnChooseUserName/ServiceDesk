CREATE TABLE ticketing.category (
    id               UUID PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(1000) NOT NULL,
    default_sla_hours INTEGER NOT NULL,
    CONSTRAINT uk_category_name UNIQUE (name),
    CONSTRAINT ck_category_default_sla_hours CHECK (default_sla_hours > 0)
);

CREATE TABLE ticketing.ticket (
    id           UUID PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    description  TEXT NOT NULL,
    priority     VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    category_id  UUID NOT NULL,
    requester_id UUID NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ticket_category
        FOREIGN KEY (category_id) REFERENCES ticketing.category (id),
    CONSTRAINT fk_ticket_requester
        FOREIGN KEY (requester_id) REFERENCES ticketing.app_user (id),
    CONSTRAINT ck_ticket_priority
        CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT ck_ticket_status
        CHECK (status = 'NEW')
);

INSERT INTO ticketing.category (id, name, description, default_sla_hours)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'Hardware',
        'Laptops, monitors, peripherals, and other equipment', 24),
    ('10000000-0000-0000-0000-000000000002', 'Software',
        'Applications, operating systems, and software access', 24),
    ('10000000-0000-0000-0000-000000000003', 'Access',
        'Accounts, permissions, and authentication issues', 8);
