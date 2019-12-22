package org.ase.fourwins.event;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaPublisher implements EventPublisher {

  private static final String KAFKA_CLIENT_ID = "MoveTrackingGameListener";
  private static final long PARTITION = 1l;
  private static final String TOPIC = "my-example-topic";
  private static final String BOOTSTRAP_SERVERS = "localhost:9092,localhost:9093,localhost:9094";
  private final Producer<Long, String> producer;

  public KafkaPublisher() {
    this.producer = createProducer();
  }

  private Producer<Long, String> createProducer() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
    props.put(ProducerConfig.CLIENT_ID_CONFIG, KAFKA_CLIENT_ID);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return new KafkaProducer<>(props);
  }

  @Override
  public void sendMessage(String message) {
    try {
      final ProducerRecord<Long, String> record = new ProducerRecord<>(TOPIC, PARTITION, message);
      RecordMetadata metadata = producer.send(record)
          .get();
      System.out.printf("sent record(key=%s value='%s')" + " metadata(partition=%d, offset=%d)\n",
          record.key(), record.value(), metadata.partition(), metadata.offset());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } finally {
      producer.flush();
      producer.close();
    }
  }

  public static void main(String[] args) throws Exception {
    new KafkaPublisher().sendMessage("test");
  }
}
