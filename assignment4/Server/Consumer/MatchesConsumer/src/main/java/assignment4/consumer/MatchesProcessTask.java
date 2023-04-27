package assignment4.consumer;

import static assignment4.config.constant.LoadTestConfig.CONSUMER_BATCH_UPDATE_SIZE;

import assignment4.config.constant.LoadTestConfig;
import assignment4.config.constant.MongoConnectionInfo;
import assignment4.config.datamodel.SwipeDetails;
import com.google.gson.Gson;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.MongoException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MatchesProcessTask extends ProcessTask{
  private Map<Integer, Set<Integer>> matchesMap = new HashMap<>();
  private static final Gson gson = new Gson();
  private final int[] batch_cnt = {0};
  private MongoCollection<Document> matchesCollection;

  public MatchesProcessTask(TopicPartition partition,
      List<ConsumerRecord<String, String>> partitionRecords, MongoClient mongoClient) {
    super(partition, partitionRecords, mongoClient);

    // Connect to MongoDB
    MongoDatabase database = mongoClient.getDatabase(MongoConnectionInfo.DATABASE);
    this.matchesCollection = database.getCollection(MongoConnectionInfo.MATCH_COLLECTION);
    this.matchesCollection.withWriteConcern(new WriteConcern(LoadTestConfig.CONSUMER_DB_WRITE_CONCERN,LoadTestConfig.CONSUMER_DB_WRITE_TIMEOUT)).withReadConcern(new ReadConcern(
        LoadTestConfig.CONSUMER_DB_READ_CONCERN_LEVEL));
  }


  protected void putToBatchMap(ConsumerRecord<String, String> record) {
    // Convert msg raw string to SwipeDetails
    SwipeDetails swipeDetails = gson.fromJson(record.value(), SwipeDetails.class);
    System.out.println("dir:" + swipeDetails.getDirection());
    Integer swiperId = Integer.valueOf(swipeDetails.getSwiper());
    Integer swipeeId = Integer.valueOf(swipeDetails.getSwipee());

    if (swipeDetails.getDirection().equals(SwipeDetails.RIGHT)) {
      this.matchesMap.putIfAbsent(swiperId, new HashSet<>());
      this.matchesMap.get(swiperId).add(swipeeId);
    }
    System.out.println( "MatchesProcessTask thread Name = " + Thread.currentThread().getName() + " has put " + "swiperId: "+ swiperId + " swipeeId: " + swipeeId + "to local map");
    this.batch_cnt[0] ++;

  }


  @Override
  protected boolean batchUpdateToDB(boolean force) {
    if (this.batch_cnt[0] < CONSUMER_BATCH_UPDATE_SIZE && !force) {
      return false;
    }
    // Update DB Matches Collection
    List bulkOps = new ArrayList<>();
    //System.out.println("Matches map size:" + this.matchesMap.size());

    // Edge case: if none of the swipe directions is right(<--> like <--> match).
    // then there is no match at all.
    // So matchesMap will be empty -> bulkOps will be empty -> DB write error
    if (this.matchesMap.size() == 0) {
      return true;
    }

    for (Map.Entry<Integer, Set<Integer>> entry: this.matchesMap.entrySet()) {
      Integer swiperId = entry.getKey();
      Set<Integer> matches = entry.getValue();

      Bson filter = Filters.eq("_id", swiperId);
      Bson initUpdate = Updates.setOnInsert("matches",  new ArrayList<>());
      Bson addUpdate = Updates.addEachToSet("matches", new ArrayList<>(matches));

      UpdateOneModel<Document> initUpdateModel = new UpdateOneModel<>(filter, initUpdate,
          new UpdateOptions().upsert(true));
      UpdateOneModel<Document> addUpdateModel = new UpdateOneModel<>(filter, addUpdate,
          new UpdateOptions().upsert(false));
      // false -> Ensure that if no document matches the filter, a new document won't be inserted
      // with the specified value (bc the specified value is not an initial value -> empty list.)

      bulkOps.add(initUpdateModel);
      bulkOps.add(addUpdateModel);
    }
    //System.out.println("maps size:" + matchesMap.size() + " " + statsMap.size());

    System.out.println("Starts BULK WRITE to Mongo, Thread:" + Thread.currentThread().getName());
    // NOTE: Try until bulkWrite succeed!!
    while (true) {
      try {
        // TODO: To be more accurate, the bulkWrite here should have multi-document transactions, to
        //  ensure "all or nothing",to match the commit logic in ProcessTask. Otherwise, there
        //  can be duplicate writes if bulkWrite fail in the middle.

        BulkWriteResult result = this.matchesCollection.bulkWrite(bulkOps);
        System.out.println("thread Name = " + Thread.currentThread().getName() + "\nBulk write to Matches:" +
            "\ninserted: " + result.getInsertedCount() +
            "\nupdated: " + result.getModifiedCount() +
            "\ndeleted: " + result.getDeletedCount() +
            "\nHashmap id count: " + this.matchesMap.size());
        return true;
      } catch (MongoException me) {
        System.out.println("thread Name = " + Thread.currentThread().getName() + ": Bulk write to Matches failed due to an error: " + me);
      }
    }
  }

  @Override
  protected  void resetBatchMap() {
    // Reset map and batch_cnt
    this.matchesMap = new HashMap<>();
    this.batch_cnt[0] = 0;
  }

}
