package assignment4.consumer;

import assignment4.config.constant.KafkaConnectionInfo;
import com.mongodb.client.MongoClient;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

public class StatsConsumerThread extends ConsumerThread {

  public StatsConsumerThread() {
    super(KafkaConnectionInfo.STATS_TOPIC, KafkaConnectionInfo.STATS_CONSUMER_GROUP_ID);
  }

  @Override
  protected ProcessTask createProcessTask(TopicPartition partition, List<ConsumerRecord<String, String>> partitionRecords,
      MongoClient mongoClient) {
    return new StatsProcessTask(partition, partitionRecords, mongoClient);
  }

  public static void main(String[] args) {
    new Thread(new StatsConsumerThread()).start();
  }
}
