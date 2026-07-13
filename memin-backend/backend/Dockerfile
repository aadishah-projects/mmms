# Build stage
FROM eclipse-temurin:22-jdk-jammy AS build

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests -Dhttps.protocols=TLSv1.2

# Runtime stage
FROM eclipse-temurin:22-jre-jammy

WORKDIR /app

# Copy only the built jar from build stage
COPY --from=build /app/target/mmms-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "app.jar"]
