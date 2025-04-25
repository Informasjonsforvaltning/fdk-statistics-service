# FDK Statistics Service

This application provides an API for the reports pages on [data.norge.no](https://data.norge.no/reports).

For a broader understanding of the system’s context, refer to
the [architecture documentation](https://github.com/Informasjonsforvaltning/architecture-documentation) wiki. For more
specific context on this application, see the **Portal** subsystem section.

## Getting Started

These instructions will give you a copy of the project up and running on your local machine for development and testing
purposes.

### Prerequisites

Ensure you have the following installed:

- Java 21
- Maven
- Docker

### Generate sources

Kafka messages are serialized using Avro. Avro schemas are located in ```kafka/schemas```. To generate sources from Avro
schema, run the following command:

```
mvn generate-sources    
```

### Running locally

#### Clone the repository

```sh
git clone https://github.com/Informasjonsforvaltning/fdk-statistics-service.git
cd fdk-statistics-service
```

#### Start PostgreSQL database, Kafka cluster and setup topics/schemas

Topics and schemas are set up automatically when starting the Kafka cluster. Docker compose uses the scripts
```create-topics.sh``` and ```create-schemas.sh``` to set up topics and schemas.

```
docker-compose up -d
```

If you have problems starting kafka, check if all health checks are ok. Make sure number at the end (after 'grep')
matches desired topics.

#### Start statistics service

Start statistics service locally using maven. Use Spring profile **dev**.

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Produce messages

Check if schema id is correct in the script. This should be 1 if there is only one schema in your registry.

```
sh ./kafka/produce-rdf-parse-events.sh
sh ./kafka/produce-dataset-remove-events.sh
```

### API Documentation (OpenAPI)

Once the application is running locally, the API documentation can be accessed
at http://localhost:8080/swagger-ui/index.html

### Running tests

```sh
mvn verify
```
