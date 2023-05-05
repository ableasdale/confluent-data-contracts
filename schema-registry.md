# Using Schema Registry

## Creating a schema without rules

````
curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    --data '{"schema": "{\"type\": \"string\"}"}' \
    http://localhost:8081/subjects/message-value/versions
````

## Let me introduce you the schema

````
{
  "type": "record",
  "namespace": "org.matias",
  "name": "Message",
  "fields": [
    {"name": "greet", "type": "string"}
  ]
}
````

## Let's generate the schema in JSON (jq power!)

````
echo '{
  "type": "record",
  "namespace": "org.matias",
  "name": "Message",
  "fields": [
    {"name": "greet", "type": "string"}
  ]
}' | jq '. | {schema: tojson}'
````

## Evolving the schema with rules

````
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
````


## Retrieving subjects

````
curl --silent -X GET http://localhost:8081/subjects | jq
````

## Retrieving a specific schema

````
curl --silent -X GET http://localhost:8081/subjects/message-value/versions/1 | jq
````