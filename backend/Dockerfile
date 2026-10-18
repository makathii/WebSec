FROM eclipse-temurin:21-jdk AS build

RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src

RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --no-create-home app

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
RUN mkdir uploads && chown app:app uploads

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
