package assignment4.consumer;

import static assignment4.config.constant.LoadTestConfig.CONSUMER_BATCH_UPDATE_SIZE;

import assignment4.config.constant.LoadTestConfig;
import assignment4.config.constant.MongoConnectionInfo;
import assignment4.config.datamodel.SwipeDetails;
import com.google.gson.Gson;
import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.bson.Document;
import org.bson.conversions.Bson;

public class StatsProcessTask extends ProcessTask {
  private Map<Integer, int[]>  statsMap = new HashMap<>();
  private static final Gson gson = new Gson();
  private final int[] batch_cnt = {0};
  private MongoCollection<Document> statsCollection;

  public StatsProcessTask(TopicPartition partition,List<ConsumerRecord<String, String>> partitionRecords,
      MongoClient mongoClient) {
    super(partition, partitionRecords, mongoClient);

    // Connect to MongoDB
    MongoDatabase database = mongoClient.getDatabase(MongoConnectionInfo.DATABASE);
    this.statsCollection = database.getCollection(MongoConnectionInfo.STATS_COLLECTION);
    this.statsCollection.withWriteConcern(new WriteConcern(LoadTestConfig.CONSUMER_DB_WRITE_CONCERN,LoadTestConfig.CONSUMER_DB_WRITE_TIMEOUT)).withReadConcern(new ReadConcern(
        LoadTestConfig.CONSUMER_DB_READ_CONCERN_LEVEL));
  }

  @Override
  protected void putToBatchMap(ConsumerRecord<String, String> record) {
    // Convert msg raw string to SwipeDetails
    SwipeDetails swipeDetails = gson.fromJson(record.value(), SwipeDetails.class);
    System.out.println("dir:" + swipeDetails.getDirection());
    Integer swiperId = Integer.valueOf(swipeDetails.getSwiper());
    Integer swipeeId = Integer.valueOf(swipeDetails.getSwipee());


    this.statsMap.putIfAbsent(swiperId, new int[] {0,0});
    if (swipeDetails.getDirection().equals(SwipeDetails.RIGHT)) {
      this.statsMap.get(swiperId)[0] ++;
    } else {
      this.statsMap.get(swiperId)[1] ++;
    }
    System.out.println( "StatsProcessTask thread Name = " + Thread.currentThread().getName() + " Received '" + "swiperId: "+ swiperId + " swipeeId: " + swipeeId);
    this.batch_cnt[0] ++;

  }


  @Override
  protected boolean batchUpdateToDB(boolean force) {
    if (this.batch_cnt[0] < CONSUMER_BATCH_UPDATE_SIZE && !force) {
      return false;
    }

    // Update DB Stats Collection
    System.out.println("!! Stats Map: " + this.statsMap.toString());
    List bulkOps = new ArrayList<>();

    for (Map.Entry<Integer, int[]> entry: this.statsMap.entrySet()) {
      Integer swiperId = entry.getKey();
      int[] stats = entry.getValue();
      int likes = stats[0];
      int dislikes = stats[1];

      Bson filter = Filters.eq("_id", swiperId);
      // setOnInsert: If the document already exists, this field will not be modified.
      // Only if a new document is inserted as a result of an update operation, will the field be specified the given value.
      Bson insertIfAbsent = Updates.combine(Updates.setOnInsert("likes", 0),
          Updates.setOnInsert("dislikes", 0));
      Bson incUpdate = Updates.combine(
          Updates.inc("likes", likes),
          Updates.inc("dislikes", dislikes));

      UpdateOneModel<Document> insertIfAbsentModel = new UpdateOneModel<>(filter, insertIfAbsent,
          new UpdateOptions().upsert(true));
      UpdateOneModel <Document> incUpdateModel = new UpdateOneModel<>(filter, incUpdate,
          new UpdateOptions().upsert(false));
      // false -> Ensure that if no document matches the filter, a new document won't be inserted
      // with the specified value (bc the specified value is the amount to increment, not an initial value.)

      bulkOps.add(insertIfAbsentModel);
      bulkOps.add(incUpdateModel);
    }

    // NOTE: Try until bulkWrite succeed!!
    while (true) {
      try {
        // TODO: To be more accurate, the bulkWrite here should have multi-document transactions, to
        //  ensure "all or nothing",to match the commit logic in ProcessTask. Otherwise, there
        //  can be duplicate writes if bulkWrite fail in the middle.

        BulkWriteResult result = this.statsCollection.bulkWrite(bulkOps);
        System.out.println("thread Name = " + Thread.currentThread().getName() + "\nBulk write to Stats:" +
            "\ninserted: " + result.getInsertedCount() +
            "\nupdated: " + result.getModifiedCount() +
            "\ndeleted: " + result.getDeletedCount() +
            "\nHashmap id count: " + this.statsMap.size());
        return true;
      } catch (MongoException me) {
        System.out.println("thread Name = " + Thread.currentThread().getName() + ": Bulk write to Stats failed due to an error: " + me);
      }
    }
  }


  @Override
  protected void resetBatchMap() {
    // Reset map and batch_cnt
    this.statsMap = new HashMap<>();
    this.batch_cnt[0] = 0;
  }
}
