-- =====================================================================
-- AgroMarket — бірінші миграция (Flyway, PostgreSQL 16)
-- 13 кесте. Байланыстар: 1:1, 1:N, M:N.
-- ER-диаграмма: docs/ER.md
-- =====================================================================

-- ---------- Анықтамалықтар ----------

CREATE TABLE regions (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- ---------- Auth (US-01, US-02) ----------

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    phone         VARCHAR(30),
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('BUYER', 'FARMER', 'COURIER', 'MODERATOR')),
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

-- 1:1 — әр фермерде бір профиль
CREATE TABLE farmer_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    region_id           BIGINT       NOT NULL REFERENCES regions (id),
    farm_name           VARCHAR(150) NOT NULL,
    verification_status VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                        CHECK (verification_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    document_url        VARCHAR(500),
    reject_reason       VARCHAR(500),
    rating              NUMERIC(2, 1) CHECK (rating BETWEEN 1 AND 5),
    -- REJECTED болса, себеп міндетті (US-02)
    CONSTRAINT reject_reason_required CHECK (verification_status <> 'REJECTED' OR reject_reason IS NOT NULL)
);

-- ---------- Catalog (US-03, US-04) ----------

-- 1:N — бір фермерде көп өнім
CREATE TABLE products (
    id                         BIGSERIAL PRIMARY KEY,
    farmer_id                  BIGINT        NOT NULL REFERENCES farmer_profiles (id) ON DELETE CASCADE,
    category_id                BIGINT        NOT NULL REFERENCES categories (id),
    region_id                  BIGINT        NOT NULL REFERENCES regions (id),
    name                       VARCHAR(150)  NOT NULL,
    price_per_kg               NUMERIC(12, 2) NOT NULL CHECK (price_per_kg > 0),
    stock_kg                   NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (stock_kg >= 0),
    organic                    BOOLEAN       NOT NULL DEFAULT FALSE,
    photo_url                  VARCHAR(500),
    -- US-06: көтерме жеңілдік (50 кг-нан бастап)
    wholesale_discount_percent NUMERIC(5, 2) NOT NULL DEFAULT 0 CHECK (wholesale_discount_percent BETWEEN 0 AND 100),
    -- US-07: маусымдық жеңілдік
    seasonal_discount_percent  NUMERIC(5, 2) CHECK (seasonal_discount_percent BETWEEN 0 AND 100),
    seasonal_from              DATE,
    seasonal_to                DATE,
    created_at                 TIMESTAMP     NOT NULL DEFAULT now(),
    CONSTRAINT seasonal_period_valid CHECK (seasonal_from IS NULL OR seasonal_to >= seasonal_from)
);

CREATE INDEX idx_products_filter ON products (category_id, region_id, price_per_kg);
CREATE INDEX idx_products_farmer ON products (farmer_id);

-- ---------- Cart (US-05) ----------

-- 1:1 — әр сатып алушыда бір себет
CREATE TABLE carts (
    id       BIGSERIAL PRIMARY KEY,
    buyer_id BIGINT NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE
);

-- M:N — себет ↔ өнім (cart_items арқылы)
CREATE TABLE cart_items (
    cart_id     BIGINT        NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id  BIGINT        NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    quantity_kg NUMERIC(10, 2) NOT NULL CHECK (quantity_kg > 0),
    PRIMARY KEY (cart_id, product_id)
);

-- ---------- Pricing (US-07) ----------

CREATE TABLE promo_codes (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    percent     NUMERIC(5, 2) NOT NULL CHECK (percent > 0 AND percent <= 100),
    valid_until DATE         NOT NULL
);

-- ---------- Orders (US-08, US-09) ----------

-- 1:N — бір сатып алушыда көп тапсырыс
CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    buyer_id        BIGINT        NOT NULL REFERENCES users (id),
    promo_code_id   BIGINT        REFERENCES promo_codes (id),
    delivery_region VARCHAR(100)  NOT NULL,
    delivery_method VARCHAR(20)   NOT NULL CHECK (delivery_method IN ('PICKUP', 'CITY_COURIER', 'INTERCITY')),
    delivery_cost   NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (delivery_cost >= 0),
    payment_method  VARCHAR(10)   NOT NULL CHECK (payment_method IN ('CARD', 'CASH')),
    items_total     NUMERIC(12, 2) NOT NULL CHECK (items_total >= 0),
    total           NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    status          VARCHAR(20)   NOT NULL DEFAULT 'CREATED'
                    CHECK (status IN ('CREATED', 'AWAITING_CASH', 'PAID', 'PAYMENT_FAILED')),
    created_at      TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_buyer ON orders (buyer_id);

-- 1:N — бір тапсырыс бірнеше фермерге бөлінеді
CREATE TABLE sub_orders (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    farmer_id       BIGINT        NOT NULL REFERENCES farmer_profiles (id),
    courier_id      BIGINT        REFERENCES users (id),
    total           NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    delivery_status VARCHAR(20)   NOT NULL DEFAULT 'NEW'
                    CHECK (delivery_status IN ('NEW', 'ASSIGNED', 'PICKED_UP', 'DELIVERED')),
    UNIQUE (order_id, farmer_id)
);

CREATE INDEX idx_sub_orders_farmer ON sub_orders (farmer_id);

-- M:N — тапсырыс ↔ өнім (order_items арқылы)
CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    sub_order_id BIGINT        NOT NULL REFERENCES sub_orders (id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products (id),
    product_name VARCHAR(150)  NOT NULL, -- тапсырыс кезіндегі атауы (кейін өзгерсе де сақталады)
    quantity_kg  NUMERIC(10, 2) NOT NULL CHECK (quantity_kg > 0),
    base_price   NUMERIC(12, 2) NOT NULL CHECK (base_price >= 0),
    total        NUMERIC(12, 2) NOT NULL CHECK (total >= 0)
);

-- ---------- Payments (US-10) ----------

-- 1:1 — әр тапсырысқа бір төлем
CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT        NOT NULL UNIQUE REFERENCES orders (id) ON DELETE CASCADE,
    provider       VARCHAR(20)   NOT NULL CHECK (provider IN ('stripe', 'cash')),
    transaction_id VARCHAR(100)  NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    status         VARCHAR(20)   NOT NULL CHECK (status IN ('PAID', 'PENDING', 'FAILED')),
    created_at     TIMESTAMP     NOT NULL DEFAULT now()
);

-- ---------- Reviews (US-13) ----------

-- 1:1 — бір ішкі тапсырысқа бір пікір
CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    sub_order_id BIGINT    NOT NULL UNIQUE REFERENCES sub_orders (id) ON DELETE CASCADE,
    buyer_id     BIGINT    NOT NULL REFERENCES users (id),
    farmer_id    BIGINT    NOT NULL REFERENCES farmer_profiles (id),
    rating       SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT now()
);

-- ---------- Notifications (US-02, US-11) ----------

CREATE TABLE notifications (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    text       VARCHAR(500) NOT NULL,
    is_read    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_user ON notifications (user_id, is_read);
