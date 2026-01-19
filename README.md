# Scheduler

Class & Service Scheduling Application built with Spring Boot and RavenDB.

A live instance is available at: https://scheduler.mcquarrie.cc

## Features

- **Class & Service Scheduling**: Manage bookings for various classes and services.
- **RavenDB Integration**: Scalable NoSQL database for document storage.
- **Spring Security**: Secure access with role-based permissions (e.g., ROLE_ADMIN).
- **Thymeleaf UI**: Server-side rendered templates for a responsive user interface.
- **Multi-tenancy Support**: Automatically handles different databases based on the `X-Forwarded-Host` header.
- **Jasypt Encryption**: Secure handling of sensitive configuration properties.
- **SSE Support**: Server-Sent Events for real-time updates.

## Technology Stack

- **Java 21**
- **Spring Boot 3.5.4**
- **RavenDB 5.4.4**
- **Thymeleaf**
- **Lombok**
- **Jasypt**
- **Caffeine Cache**
- **Docker & Kubernetes**

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- A running RavenDB instance (default: `http://raven.dell.mcquarrie.cc`)

### Configuration

The application uses Jasypt for property encryption. You must provide the master password at runtime via the `JASYPT_ENCRYPTOR_PASSWORD` environment variable or system property.

#### Encrypting New Properties

To encrypt a new property value:
1. Set `JASYPT_ENCRYPTOR_PASSWORD`.
2. Run the helper tool:
   ```bash
   mvn -q -Dexec.mainClass=org.gpc4j.web.util.JasyptEncryptorTool \
          -Dexec.args="your-plain-secret" exec:java
   ```
3. Copy the output `ENC(...)` into `application.yml`.

### Build

To build the project and generate the JAR file:
```bash
mvn clean package
```

### Run Locally

```bash
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-password
mvn spring-boot:run
```
The application will be available at `http://localhost:8085`.

## Docker and Kubernetes

### Docker

A `Dockerfile` is provided for containerization. It uses `amazoncorretto:25` as the base image.

Build the image:
```bash
docker build -t webtemplate:latest .
```

### Kubernetes

The project includes `k8s.yml` for deployment to a Kubernetes cluster. It defines:
- **Deployment**: Single replica with health checks and Jasypt secret integration.
- **Service**: Exposes the application on port 8085.
- **Ingress**: Configures Nginx ingress with TLS support via Let's Encrypt.
- **HorizontalPodAutoscaler**: Scales up to 4 replicas based on CPU utilization.

## Project Structure

- `src/main/java`: Backend logic (Controllers, Services, Configs, DTOs).
- `src/main/resources/templates`: Thymeleaf templates for the UI.
- `src/main/resources/static`: Static assets (JS, CSS, Images).
- `k8s.yml`: Kubernetes deployment manifest.
- `Dockerfile`: Docker image definition.
- `.editorconfig`: Code style configuration (e.g., 85 character line limit for Java).
