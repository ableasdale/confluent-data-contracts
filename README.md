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

## Wait for Schema Registry to startup

```bash
curl localhost:8081/schemas/types
```

When it's ready, you should see a list of supported schema types returned:

```bash
["JSON","PROTOBUF","AVRO"]
```

## Register Schema and associated ruleSet

```bash
curl --silent -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{
            "schemaType": "AVRO",
            "schema": "{\"type\":\"record\",\"namespace\":\"org.matias\",\"name\":\"Message\",\"fields\":[{\"name\":\"greet\",\"type\":\"string\"}]}",
            "metadata": {
                "properties" : {
                    "owner": "Firstname Surname",
                    "email": "emailaddress@example.com"
                },
                "tags": {
                    "message.greet": [ "PII" ]
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
                    "onFailure": "DLQ"
                  }
                ]}}' \
    http://localhost:8081/subjects/message-value/versions
```

You should see an id returned on success:

```bash
{"id":1}%
```

## Retrieving a specific schema

````bash
curl --silent -X GET http://localhost:8081/subjects/message-value/versions/1 | jq
````

## Run the application

### Success Path

Run the application with the following arguments:

```bash
java -jar build/libs/kafka-producer-application-0.0.1.jar configuration/dev.properties input.txt
```

For the input.txt file, 2 records should be created:

```bash
12:24:14.502 INFO  i.confluent.developer.KafkaAvroProducerApplication.lambda$printMetadata$0:69 - Record written to offset 0 timestamp 1683285854087
12:24:14.502 INFO  i.confluent.developer.KafkaAvroProducerApplication.lambda$printMetadata$0:69 - Record written to offset 1 timestamp 1683285854480
```

### Failure Path

Run the application with the following arguments:

```bash
java -jar build/libs/kafka-producer-application-0.0.1.jar configuration/dev.properties input-fail.txt
```

You should see the following failure scenario (accompanied by the message going to a DLQ):

```bash
14:22:51.798 INFO  io.confluent.kafka.schemaregistry.rules.DlqAction.lambda$run$0:76 - Sent message to dlq topic checkLenDLQ
[...]
Caused by: org.apache.kafka.common.errors.SerializationException: Rule failed: checkLen
[...]
Caused by: io.confluent.kafka.schemaregistry.rules.RuleException: Expr 'size(message.greet) == 4' failed
```

### Consume

Connect to the Schema Registry container:

```bash
docker exec -it confluent-data-contracts-schema-registry-1 bash
```

Use the Avro Console Consumer to read from the `message` topic:

```bash
kafka-avro-console-consumer --topic message --bootstrap-server kafka:9092 --property schema.registry.url=http://localhost:8081
```

## Further Reading

- <https://docs.confluent.io/platform/current/schema-registry/fundamentals/data-contracts.html>
- <https://github.com/google/cel-spec>
- <https://docs.confluent.io/cloud/current/get-started/schema-registry.html#cloud-sr-config>