# FDK Statistics Service

## Requirements

- maven
- java 21
- docker
- docker-compose

## Generate sources

Kafka messages are serialized using Avro. Avro schemas are located in the kafka/schemas directory.
To generate sources from Avro schema, run the following command:

```
mvn generate-sources    
```

## Run tests

```
mvn test
```

## Run locally

### Start PostgreSQL database, Kafka cluster and setup topics/schemas

Topics and schemas are set up automatically when starting the Kafka cluster.
Docker compose uses the scripts create-topics.sh and create-schemas.sh to set up topics and schemas.

```
docker-compose up -d
```

If you have problems starting kafka, check if all health checks are ok.
Make sure number at the end (after 'grep') matches desired topics.

### Start statistics service
Start statistics service locally using maven. Use Spring profile **dev**.

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Produce messages
Check if schema id is correct in the script. This should be 1 if there
is only one schema in your registry.

```
sh ./kafka/produce-rdf-parse-events.sh
sh ./kafka/produce-dataset-remove-events.sh
```
