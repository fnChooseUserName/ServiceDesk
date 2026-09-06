-- Introduces user identity/role storage for S2 (authentication & role identity).
CREATE TABLE ticketing.app_user (
    id            UUID PRIMARY KEY,
    full_name     VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('REQUESTER', 'AGENT', 'ADMIN'))
);
