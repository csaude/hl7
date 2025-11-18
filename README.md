# HL7 Tool (Web Client)

## Overview
The **HL7 Tool** is a web interface designed for lab technicians to extract patient demographic data from an EPTS/OpenMRS server. It acts as a client that communicates with the **HL7 Sync Middleware** to request, monitor, and download encrypted HL7 files.

## Project Structure
This is a Maven project composed of two modules:
* **configurer:** A helper app allowing administrators to set encrypted configuration properties.
* **web:** The main Spring Boot web application used by technicians.

## Prerequisites
* **Java:** JDK 8 (strictly required for this legacy codebase).
* **Maven:** 3.8+
* **Middleware:** A running instance of the [HL7Sync Middleware](https://github.com/csaude/hl7sync) (usually on port 8081).

---

## 🛠️ Development Environment Setup

Running this application locally requires bypassing the production-grade encryption mechanisms. Follow these steps to set up a local dev environment.

### 1. Disable Encryption Loader
The application is configured to load properties from an encrypted file by default. For local development, you must disable this listener.

Open `mz.org.fgh.hl7.web.Hl7Application.java` and comment out the `.listeners(...)` lines:

```java
@SpringBootApplication
public class Hl7Application extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
       return application.sources(Hl7Application.class);
             // .listeners(new EncryptedEnvironmentLoader()); // <-- COMMENT THIS OUT
    }

    public static void main(String[] args) {
       new SpringApplicationBuilder(Hl7Application.class)
             // .listeners(new EncryptedEnvironmentLoader()) // <-- COMMENT THIS OUT
             .run(args);
    }
}
```

### 2. Create Local Configuration

Create a plain text `application.properties` file in `web/src/main/resources/`. Use the following template to connect to your local Middleware and OpenMRS:

```properties
# Server Port
server.port=8083

# App Credentials (Local Auth)
app.username=admin
app.password=Admin123

# OpenMRS Connection
app.openmrs.login=false
openmrs.url=http://localhost:8080/openmrs/
openmrs.username=admin
openmrs.password=Admin123

# Database (For the Web App's internal needs)
spring.datasource.url=jdbc:mysql://localhost:3306/hl7_web_db
spring.datasource.username=root
spring.datasource.password=root

# Middleware API Endpoints (Assuming HL7Sync runs on 8081)
# If using OpenHIM, point these to the OpenHIM port (e.g., 5001)
hl7.generate.api=http://localhost:8081/api/demographics/generate
hl7.downloadFile.api=http://localhost:8081/api/demographics/download/
hl7.fileStatus.api=http://localhost:8081/api/demographics/status/
hl7.health.api=http://localhost:8081/api/demographics/health
hl7.generatedHl7Files.api=http://localhost:8081/api/demographics/getGeneratedHL7Files/

# Dummy Keys (Required to prevent startup crashes in Dev)
app.disa.secretKey=dev_dummy_key
app.csaude.secretKey=dev_dummy_key

# File Settings
app.hl7.folder=/opt/hl7/
app.hl7.filename=Patient_Demographic_Data
app.hl7.hidden.filename=.Hidden.Patient_Demographic_Data
hl7.standard.file.name=Patient_Demographic_Data.hl7.enc
hl7.metadata.name=.metadata.json
```

### 3. State Management (config.json)

The application persists the scheduler state (Job ID, Last Run Time, Selected Facilities) in a `config.json` file in the root execution directory. If you want to simulate a "Fresh Install," simply delete this file.

### Production Configuration (Encryption)

In a production environment, the encryption listener is enabled. A keyStore (`app.keyStore`) and password (`app.keyStore.password`) must be provided via environment variables or system properties. The keyStore must contain:
- DISA secret key (disaSecretKeyAlias)
- C-Saúde secret key (cSaudeSecretKeyAlias)

## Configurer Module (Frontend)

The configurer module uses React. To develop the frontend:
1. Install nodejs v20 and npm (using nvm is recommended).
2. Run watch mode for hot-reloading assets:

   ```bash
   npm run watch
   ```
