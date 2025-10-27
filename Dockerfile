
#Sử dụng một image name chính xác, kết hợp Maven 3.9.6 và Java 21 (Temurin)
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml file to download dependencies
COPY pom.xml .

# Download all dependencies to take advantage of Docker layer caching
RUN mvn dependency:go-offline

# Copy your source code
COPY src ./src

# Package the application, skipping the tests to build faster
RUN mvn package -DskipTests

# STAGE 2: Create a lightweight image to run the application
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the executable JAR file from the build stage
COPY --from=build /app/target/trungtambaohanh-0.0.1-SNAPSHOT.jar .

# Expose the port your app runs on
EXPOSE 8080

# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]