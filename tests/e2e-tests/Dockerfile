# Start from a base image with Java (change the version if needed)
FROM azul/zulu-openjdk-alpine:19-latest

# Set the working directory inside the container
WORKDIR /app

# Copy your Gradle Wrapper, build.gradle.kts, and your src directory into the Docker image
COPY ./ /app/

# Grant execution permissions to the Gradle Wrapper
RUN chmod +x /app/gradlew

# Build the project inside the container. This will download all required dependencies.
RUN ./gradlew clean testClasses

# Command to run the tests
CMD ["./gradlew", "test"]