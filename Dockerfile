# syntax=docker/dockerfile:1

# ===== Build stage =====
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom and resolve dependencies first (better build caching)
COPY pom.xml ./
RUN mvn -B -q -e -DskipTests dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn -B -q -DskipTests package

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
ENV JAVA_OPTS=""
WORKDIR /opt/app

# Copy the fat jar from build stage
COPY --from=build /app/target/jira-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
