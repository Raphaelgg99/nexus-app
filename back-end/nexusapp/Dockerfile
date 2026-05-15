FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S nexus && adduser -S nexus -G nexus
COPY --from=build /app/target/*.jar app.jar

USER nexus
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
