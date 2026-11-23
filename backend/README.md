# AgroMarket — Backend (Java)

12 user story-дің бизнес-логикасы және 6 design pattern. Java 17+, Maven, JUnit 5.
Келесі кезеңде Spring Boot-қа көшіріледі (сервистер `@Service`, тесттер өзгермейді).

## Іске қосу

**VS Code арқылы** (Extension Pack for Java орнатылған болуы керек):
- `src/main/java/kz/agromarket/Demo.java` файлын ашып, **Run** батырмасын басу — 12 user story-дің сценарийі;
- сол жақтағы **Testing** (колба белгісі) бөлімінен барлық тесттерді іске қосу.

**Терминал арқылы** (Maven орнатылған болса):

```bash
cd backend
mvn test                  # 40 unit-тест
mvn compile exec:java     # 12 user story-дің демосы
```

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
