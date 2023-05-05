# Confluent Data Contracts

Testing the new features of Confluent Platform 7.4 and Data Contracts with Schema Metadata and Schema Rules

## Setup

Start the docker-compose file:

```bash
docker-compose up
```

Build the project:

```bash
gradle clean shadowJar
```

## Register Schema and associated ruleSet

```bash
curl --silent -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{
            "schemaType": "AVRO",
            "schema": "{\"type\":\"record\",\"namespace\":\"org.matias\",\"name\":\"Message\",\"fields\":[{\"name\":\"greet\",\"type\":\"string\"}]}",
            "metadata": {
                "properties" : {
                    "owner": "Matias Cascallares",
                    "email": "mcascallaresfondevila@confluent.io"
                }
            },
            "ruleSet": {
                "domainRules": [
                  {
                    "name": "checkLen",
                    "kind": "CONDITION",
                    "type": "CEL",
                    "mode": "WRITE",
                    "expr": "size(message.greet) == 4",
                    "onFailure": "ERROR"
                  }
                ]}}' \
    http://localhost:8081/subjects/message-value/versions
```

Run the application:

```bash
java -jar build/libs/kafka-producer-application-0.0.1.jar configuration/dev.properties input.txt
```

## Further Reading

