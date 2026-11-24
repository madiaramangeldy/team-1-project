# ER-диаграмма — AgroMarket

Дерекқор: **PostgreSQL 16**. Миграциялар: **Flyway** (`backend/src/main/resources/db/migration/`).

- `V1__init.sql` — 14 кесте, байланыстар, шектеулер (constraints), индекстер
- `V2__seed_reference_data.sql` — анықтамалық деректер: өңірлер, санаттар, промокодтар

## Диаграмма

```mermaid
erDiagram
    USERS ||--o| FARMER_PROFILES : "фермер профилі"
    USERS ||--o| CARTS : "себеті"
    USERS ||--o{ ORDERS : "тапсырыс береді"
    USERS ||--o{ NOTIFICATIONS : "хабарлама алады"
    USERS ||--o{ REVIEWS : "пікір жазады"
    USERS |o--o{ SUB_ORDERS : "курьер жеткізеді"

    REGIONS ||--o{ FARMER_PROFILES : "орналасқан"
    REGIONS ||--o{ PRODUCTS : "өсірілген"
    CATEGORIES ||--o{ PRODUCTS : "санаты"

    FARMER_PROFILES ||--o{ PRODUCTS : "сатады"
    FARMER_PROFILES ||--o{ SUB_ORDERS : "ішкі тапсырыс алады"
    FARMER_PROFILES ||--o{ REVIEWS : "пікір алады"

    CARTS ||--o{ CART_ITEMS : "құрамы"
    PRODUCTS ||--o{ CART_ITEMS : "себетте"

    PROMO_CODES |o--o{ ORDERS : "қолданылады"
    ORDERS ||--|{ SUB_ORDERS : "фермерлер бойынша бөлінеді"
    ORDERS ||--o| PAYMENTS : "төлемі"
    SUB_ORDERS ||--|{ ORDER_ITEMS : "құрамы"
    PRODUCTS ||--o{ ORDER_ITEMS : "тапсырыста"
    SUB_ORDERS ||--o| REVIEWS : "пікір"

    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar role "BUYER, FARMER, COURIER, MODERATOR"
    }
    FARMER_PROFILES {
        bigint id PK
        bigint user_id FK,UK
        bigint region_id FK
        varchar farm_name
        varchar verification_status "PENDING, APPROVED, REJECTED"
        varchar document_url
        numeric rating
    }
    REGIONS {
        bigint id PK
        varchar name UK
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
    }
    PRODUCTS {
        bigint id PK
        bigint farmer_id FK
        bigint category_id FK
        bigint region_id FK
        varchar name
        numeric price_per_kg
        numeric stock_kg
        boolean organic
        varchar photo_url "MinIO"
        numeric wholesale_discount_percent
        numeric seasonal_discount_percent
    }
    CARTS {
        bigint id PK
        bigint buyer_id FK,UK
    }
    CART_ITEMS {
        bigint cart_id PK,FK
        bigint product_id PK,FK
        numeric quantity_kg
    }
    PROMO_CODES {
        bigint id PK
        varchar code UK
        numeric percent
        date valid_until
    }
    ORDERS {
        bigint id PK
        bigint buyer_id FK
        bigint promo_code_id FK
        varchar delivery_method "PICKUP, CITY_COURIER, INTERCITY"
        varchar payment_method "CARD, CASH"
        numeric total
        varchar status
    }
    SUB_ORDERS {
        bigint id PK
        bigint order_id FK
        bigint farmer_id FK
        bigint courier_id FK
        numeric total
        varchar delivery_status
    }
    ORDER_ITEMS {
        bigint id PK
        bigint sub_order_id FK
        bigint product_id FK
        numeric quantity_kg
        numeric total
    }
    PAYMENTS {
        bigint id PK
        bigint order_id FK,UK
        varchar provider "stripe, cash"
        varchar transaction_id
        varchar status
    }
    REVIEWS {
        bigint id PK
        bigint sub_order_id FK,UK
        bigint farmer_id FK
        smallint rating "1-5"
    }
    NOTIFICATIONS {
        bigint id PK
        bigint user_id FK
        varchar text
        boolean is_read
    }
```

## Байланыстар

| Түрі | Байланыс | Мағынасы |
|---|---|---|
| **1 : 1** | `users` — `farmer_profiles` | Әр фермерде бір профиль (`user_id UNIQUE`) |
| **1 : 1** | `users` — `carts` | Әр сатып алушыда бір себет |
| **1 : 1** | `orders` — `payments` | Әр тапсырысқа бір төлем |
| **1 : 1** | `sub_orders` — `reviews` | Бір ішкі тапсырысқа бір пікір (US-13) |
| **1 : N** | `farmer_profiles` → `products` | Бір фермерде көп өнім |
| **1 : N** | `categories`, `regions` → `products` | Санат пен өңір бойынша сүзу (US-04) |
| **1 : N** | `users` → `orders` | Бір сатып алушыда көп тапсырыс |
| **1 : N** | `orders` → `sub_orders` | Тапсырыс фермерлер бойынша бөлінеді (US-09) |
| **M : N** | `carts` ↔ `products` | `cart_items` арқылы: себетте көп өнім, өнім көп себетте |
| **M : N** | `orders` ↔ `products` | `sub_orders` + `order_items` арқылы |

## Дерекқор деңгейіндегі тексерулер

Бизнес-ережелер кодта ғана емес, дерекқорда да қорғалған:

- `email` бірегей — бір email-мен екі рет тіркелуге болмайды (US-01)
- `REJECTED` мәртебесінде `reject_reason` міндетті (US-02)
- `price_per_kg > 0`, `stock_kg >= 0` (US-03)
- жеңілдік пайызы 0–100 аралығында (US-06, US-07)
- `rating` 1–5 аралығында (US-13)
- рөлдер, мәртебелер, жеткізу және төлем тәсілдері тек рұқсат етілген мәндерден (`CHECK ... IN (...)`)

## Seed жоспары (демо деректер)

| Деректер | Қайда жасалады | Мазмұны |
|---|---|---|
| Өңірлер, санаттар, промокодтар | `V2__seed_reference_data.sql` (Flyway) | 4 өңір, 6 санат, 3 промокод (`AGRO10`, `WELCOME5`, мерзімі өткен `OLD5`) |
| Пайдаланушылар | `DataSeeder` класы (Spring Boot, `dev` профилі) | Модератор, 2 фермер (Алматы облысы, Астана), кафе, курьер. Құпиясөздер BCrypt-пен хэштеледі, сондықтан SQL-да емес |
| Өнімдер | `DataSeeder` | Әр фермерге 3–5 өнім: қызанақ (көтерме және маусымдық жеңілдікпен), алма, картоп т.б. |

Демо сценарийі (бақылау нүктелері үшін): кафе 2 фермерден тапсырыс береді → тапсырыс 2 ішкі тапсырысқа бөлінеді → `AGRO10` промокоды мен көтерме жеңілдік қолданылады → Stripe test mode арқылы төленеді.
