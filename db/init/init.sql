CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS inventory (
    id UUID NOT NULL,
    product_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    stock INTEGER NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT chk_inventory_stock_nonnegative CHECK (stock >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_inventory_owner_product
    ON inventory (owner_id, product_id);

CREATE TABLE IF NOT EXISTS cart_abandonments (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    cart_total NUMERIC(19,4) NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    abandoned_at TIMESTAMPTZ,
    recovered_at TIMESTAMPTZ,
    recovery_deadline TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    recovery_status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT pk_cart_abandonments PRIMARY KEY (id),
    CONSTRAINT chk_cart_total_nonnegative CHECK (cart_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_cart_owner_status_activity
    ON cart_abandonments (owner_id, status, last_activity_at);

CREATE INDEX IF NOT EXISTS idx_cart_owner_abandoned_cursor
    ON cart_abandonments (owner_id, abandoned_at, id);

INSERT INTO inventory (id, product_id, owner_id, stock, updated_at, version)
VALUES
    ('11111111-1111-4111-8111-111111111111', '22222222-2222-4222-8222-222222222222', '33333333-3333-4333-8333-333333333333', 15, NOW(), 0),
    ('44444444-4444-4444-8444-444444444444', '55555555-5555-4555-8555-555555555555', '33333333-3333-4333-8333-333333333333', 7, NOW(), 0),
    ('66666666-6666-4666-8666-666666666666', '77777777-7777-4777-8777-777777777777', '88888888-8888-4888-8888-888888888888', 3, NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO cart_abandonments (
    id, owner_id, customer_email, cart_total, last_activity_at, abandoned_at,
    recovered_at, recovery_deadline, status, recovery_status, version
)
VALUES
    (
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
        '33333333-3333-4333-8333-333333333333',
        'alice@example.com',
        149.99,
        NOW() - INTERVAL '45 minutes',
        NOW() - INTERVAL '45 minutes',
        NULL,
        NOW() + INTERVAL '24 hours',
        'ABANDONED',
        'NOT_STARTED',
        0
    ),
    (
        'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
        '33333333-3333-4333-8333-333333333333',
        'bob@example.com',
        89.50,
        NOW() - INTERVAL '10 minutes',
        NULL,
        NULL,
        NULL,
        'ACTIVE',
        'NOT_STARTED',
        0
    ),
    (
        'cccccccc-cccc-4ccc-8ccc-cccccccccccc',
        '88888888-8888-4888-8888-888888888888',
        'carol@example.com',
        210.00,
        NOW() - INTERVAL '2 hours',
        NOW() - INTERVAL '2 hours',
        NOW() - INTERVAL '30 minutes',
        NOW() - INTERVAL '1 hour',
        'RECOVERED',
        'RECOVERED',
        0
    )
ON CONFLICT (id) DO NOTHING;
