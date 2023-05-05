package io.confluent.developer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.matias.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaAvroProducerApplication {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Producer<String, Message> producer;
    final String outTopic;

    public KafkaAvroProducerApplication(final Producer<String, Message> producer,
                                        final String topic) {
        this.producer = producer;
        this.outTopic = topic;
    }

    public Future<RecordMetadata> produce(final String message) {
        final String[] parts = message.split("-");
        final String key;
        final Message value;
        if (parts.length > 1) {
            key = parts[0];
            value = new Message(parts[1]);
        } else {
            key = null;
            value = new Message(parts[0]);
        }

        final ProducerRecord<String, Message> producerRecord = new ProducerRecord<>(outTopic, key, value);
        return producer.send(producerRecord);
    }

    public void shutdown() {
        producer.close();
    }

    public static Properties loadProperties(String fileName) throws IOException {
        final Properties envProps = new Properties();
        final FileInputStream input = new FileInputStream(fileName);
        envProps.load(input);
        input.close();

        return envProps;
    }

    public void printMetadata(final Collection<Future<RecordMetadata>> metadata,
                              final String fileName) {
        metadata.forEach(m -> {
            try {
                final RecordMetadata recordMetadata = m.get();
                LOG.info("Record written to offset " + recordMetadata.offset() + " timestamp " + recordMetadata.timestamp());
            } catch (InterruptedException | ExecutionException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException(
                    "This program takes two arguments: the path to an environment configuration file and" +
                            "the path to the file with records to send");
        }

        final Properties props = KafkaAvroProducerApplication.loadProperties(args[0]);
        final String topic = props.getProperty("output.topic.name");
        final Producer<String, Message> producer = new KafkaProducer<>(props);
        final KafkaAvroProducerApplication producerApp = new KafkaAvroProducerApplication(producer, topic);

        String filePath = args[1];
        try {
            List<String> linesToProduce = Files.readAllLines(Paths.get(filePath));
            List<Future<RecordMetadata>> metadata = linesToProduce.stream()
                    .filter(l -> !l.trim().isEmpty())
                    .map(producerApp::produce)
                    .collect(Collectors.toList());
            producerApp.printMetadata(metadata, filePath);

        } catch (IOException e) {
            LOG.error("Error reading file %s due to %s %n", filePath, e);
        }
        finally {
            producerApp.shutdown();
        }
    }
}