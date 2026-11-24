# AgroMarket — Backend (Java, Spring Boot)

Java 17+, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL 16, Swagger (springdoc), JUnit 5.

## Іске қосу

**1. PostgreSQL керек.** Docker болса, бір командамен:

```bash
docker run -d --name agromarket-db -p 5432:5432 -e POSTGRES_DB=agromarket -e POSTGRES_USER=agromarket -e POSTGRES_PASSWORD=agromarket postgres:16
```

Кейін бұл `docker-compose.yml` арқылы іске қосылады.

**2. Backend.** VS Code-та `AgroMarketApplication.java` → **Run**. Іске қосылғанда Flyway кестелерді өзі жасайды.

- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/api/health

**Тесттер мен демо** (дерекқорсыз жұмыс істейді):
- VS Code → **Testing** → барлық тесттер;
- `Demo.java` → **Run** — 12 user story-дің консольдық сценарийі.

## Қабаттар

```
Controller (REST, DTO)  →  Service (бизнес-логика, паттерндер)  →  Repository (JPA)  →  PostgreSQL
```

- `config/DomainConfig.java` — сервистерді Spring DI-контейнеріне тіркеу
- `common/web/GlobalExceptionHandler.java` — барлық қателер бірыңғай JSON форматында (`ApiError`)
- `src/main/resources/db/migration/` — Flyway миграциялары

## User story → код

| User Story | Файл | Паттерн |
|---|---|---|
| US-01 Фермердің тіркелуі | `auth/AuthService.java` | — |
| US-02 Фермерді верификациялау | `auth/AuthService.java` | Observer (хабарлама) |
| US-03 Өнім қосу | `catalog/CatalogService.java`, `catalog/ProductRequest.java` | Builder |
| US-04 Каталогты сүзу | `catalog/CatalogService.java`, `catalog/CatalogFilter.java` | — |
| US-05 Себет | `cart/CartService.java` | — |
| US-06 Көтерме баға | `pricing/WholesaleDiscount.java` | **Decorator** |
| US-07 Маусымдық жеңілдік, промокод | `pricing/SeasonalDiscount.java`, `pricing/PromoCodeDiscount.java` | **Decorator** |
| US-08 Жеткізу тәсілі | `delivery/*Strategy.java`, `delivery/DeliveryStrategyFactory.java` | **Strategy**, **Factory Method** |
| US-09 Тапсырысты тексеру | `orders/OrderCheck.java`, `orders/OrderChecks.java` | **Chain of Responsibility** |
| US-10 Төлем | `payments/StripeAdapter.java`, `payments/CashOnDeliveryAdapter.java`, `payments/PaymentGatewayFactory.java` | **Adapter**, **Factory Method** |
| US-11 Төлем туралы хабарлама | `common/EventBus.java`, `notifications/NotificationService.java` | **Observer** |
| US-12 Сату дашборды | `analytics/AnalyticsService.java` | **Observer** |

Барлық файлдар `src/main/java/kz/agromarket/` ішінде.

## Құрылымы

```
backend/
├── pom.xml
└── src/
    ├── main/java/kz/agromarket/
    │   ├── common/          # қате класы, ақша, оқиғалар шинасы (Observer)
    │   ├── auth/            # US-01, US-02
    │   ├── catalog/         # US-03, US-04
    │   ├── cart/            # US-05
    │   ├── pricing/         # US-06, US-07 — Decorator
    │   ├── delivery/        # US-08 — Strategy + Factory Method
    │   ├── orders/          # US-09 — Chain of Responsibility
    │   ├── payments/        # US-10 — Adapter + Factory Method
    │   ├── notifications/   # US-11 — Observer
    │   ├── analytics/       # US-12 — Observer
    │   ├── AgroMarketApp.java   # модульдерді біріктіру (Dependency Injection)
    │   └── Demo.java            # 12 user story-дің демосы
    └── test/java/kz/agromarket/ # әр user story-ге тесттер (JUnit 5)
```
