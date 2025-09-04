# Use a Maven image to build the application
FROM maven:3-openjdk-17 AS build

# Set the working directory
WORKDIR /app

# Copy the project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Use a lean OpenJDK image for the final container
FROM openjdk:17-jdk-slim

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8082

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
