# =====================================================================
# AgroMarket backend — көп кезеңді (multi-stage) сборка
# 1-кезең: Maven арқылы jar жинау (үлкен образ, тек сборка үшін)
# 2-кезең: тек JRE + дайын jar (кіші образ, іске қосу үшін)
# =====================================================================

# ---------- 1. Сборка ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Алдымен тек pom.xml: тәуелділіктер кэштеледі, код өзгерсе қайта жүктелмейді
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY checkstyle.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- 2. Іске қосу ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Қауіпсіздік: root емес пайдаланушымен іске қосу
RUN useradd --system --uid 1001 agromarket
USER agromarket

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
