
# Stage 1: Build the application
FROM maven:3.8.4-openjdk-17-slim AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Update Alpine's system certificate store
RUN apk update \
    && apk upgrade --no-cache ca-certificates \
    && update-ca-certificates

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
