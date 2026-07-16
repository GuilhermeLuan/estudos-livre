FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.11-eclipse-temurin-21-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -B
COPY backend/src ./src
COPY --from=frontend-build /workspace/frontend/dist ./src/main/resources/static
RUN mvn package -B -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S estuda-livre && adduser -S estuda-livre -G estuda-livre
WORKDIR /app
COPY --from=backend-build --chown=estuda-livre:estuda-livre /workspace/backend/target/estuda-livre-*.jar app.jar
USER estuda-livre
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
