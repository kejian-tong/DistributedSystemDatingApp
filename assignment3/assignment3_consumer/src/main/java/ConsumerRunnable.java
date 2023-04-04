import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

public class ConsumerRunnable implements Runnable {

  private final Connection connection;
  private final MongoDatabase database;
  private final int batchSize = 100;
  private final int threadPoolSize = 40; // previously is 30
  private final AtomicInteger processedMessageCount = new AtomicInteger(0);
  private final List<Document> swipeBatch = new ArrayList<>();

  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
      threadPoolSize, // corePoolSize
      threadPoolSize, // maximumPoolSize
      0L,
      TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>()
  );

  public ConsumerRunnable(Connection connection, MongoDatabase database) {
    this.connection = connection;
    this.database = database;
//    this.matchesCollection = database.getCollection("matches");
//    this.statsCollection = database.getCollection("stats");
//    createIndexCollections();
  }

  // create index for collections
//  private void createIndexCollections() {
//    matchesCollection.createIndex(new Document("_id", 1));
//    statsCollection.createIndex(new Document("_id", 1));
//  }

  @Override
  public void run() {
    Channel channel;
    try {
      channel = connection.createChannel();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      channel.queueDeclare(Constant.QUEUE_NAME, true, false, false, null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {

      String message = new String(delivery.getBody(), "UTF-8");
      Gson gson = new Gson();
      SwipeDetails swipeDetails = gson.fromJson(message, SwipeDetails.class);
      Integer swiper = swipeDetails.getSwiper();
      Integer swipee = swipeDetails.getSwipee();
      String comment = swipeDetails.getComment();
      boolean isLike = swipeDetails.getLike();

      Document swipeDocument = new Document("swiper", swiper)
          .append("swipee", swipee)
          .append("comment", comment)
          .append("isLike", isLike);

      swipeBatch.add(swipeDocument);

      int currentCount = processedMessageCount.incrementAndGet();

      // Check if the batch size reached 100
      if (swipeBatch.size() >= batchSize) {
        flushBatch(swipeBatch);
      }
      // send ack after every batchSize processed msg
      if (currentCount % batchSize == 0) {
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
      }
    };

    try {// comment for basicConsume which is used to consume the message from the queue
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      Logger.getLogger(ConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);
    }
  }

  public void flushBatch(List<Document> swipeBatch) {
    if (!swipeBatch.isEmpty()) {
      ConcurrentLinkedQueue<Document> batchToProcess = new ConcurrentLinkedQueue<>(swipeBatch);
      swipeBatch.clear();

      // Submit the task to the thread pool
      executor.submit(() -> processMatches(batchToProcess));
    }
  }

  private void processMatches(ConcurrentLinkedQueue<Document> batchToProcess) {
    // Update user stats and insert matches in a loop
    // Perform batch write to matches and stats collections
    MongoCollection<Document> matchesCollection = database.getCollection("matches");
    MongoCollection<Document> statsCollection = database.getCollection("stats");

   // create index on "_id" field on matches and stats collections
    matchesCollection.createIndex(new Document("_id", 1));
    statsCollection.createIndex(new Document("_id", 1));

    List<WriteModel<Document>> matchesBulkOperations = new ArrayList<>();

    Map<Integer, Set<Integer>> mutualMatches = new HashMap<>();
    Map<Integer, int[]> statsMap = new HashMap<>();

    for (Document doc : batchToProcess) {
      int swiper = doc.getInteger("swiper");
      int swipee = doc.getInteger("swipee");
      boolean isLike = doc.getBoolean("isLike");

      // Update user stats
      int[] stats = statsMap.computeIfAbsent(swiper, k -> new int[]{0, 0});
      if (isLike) {
        stats[0]++;
      } else {
        stats[1]++;
      }

      // Check for a mutual match and insert it if found
      if (isLike && swiper != swipee) {
        SwipeRecord.addToLikeMap(swiper, swipee, true);
        Set<Integer> swiperRightSet = SwipeRecord.listSwiperRight.get(swiper);
        Set<Integer> swipeeRightSet = SwipeRecord.listSwipeeRight.get(swipee);
        if (swiperRightSet != null && swipeeRightSet != null && swiperRightSet.contains(swipee) && swipeeRightSet.contains(swiper)) {
          mutualMatches.computeIfAbsent(swiper, k -> new HashSet<>()).add(swipee);
          mutualMatches.computeIfAbsent(swipee, k -> new HashSet<>()).add(swiper);
        }
      }
    }

    for (Map.Entry<Integer, Set<Integer>> entry : mutualMatches.entrySet()) {
      Document query = new Document("_id", entry.getKey());
      Document update = new Document("$addToSet", new Document("matchedSwipees", new Document("$each", entry.getValue())));
      matchesBulkOperations.add(new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));
    }

    if (!matchesBulkOperations.isEmpty()) {
      matchesCollection.bulkWrite(matchesBulkOperations);
    }
    updateStatsCollection(statsCollection, statsMap);
  }

  private void updateStatsCollection(MongoCollection<Document> collection, Map<Integer, int[]> statsMap) {
    List<WriteModel<Document>> bulkOps = new ArrayList<>();

    for (Map.Entry<Integer, int[]> entry : statsMap.entrySet()) {
      Integer swiperId = entry.getKey();
      int[] stats = entry.getValue();
      int numLikes = stats[0];
      int numDislikes = stats[1];

      Bson filter = Filters.eq("_id", swiperId);
      // creates an update operation that sets the "numLikes" and "numDislikes" fields to 0 if the document doesn't exist yet (using setOnInsert)
      Bson insertIfAbsent = Updates.combine(Updates.setOnInsert("numLikes", 0),
          Updates.setOnInsert("numDislikes", 0));
      // creates an update operation that increments the "numLikes" and "numDislikes" fields by the corresponding values in likes and dislikes variables
      Bson incUpdate = Updates.combine(
          Updates.inc("numLikes", numLikes),
          Updates.inc("numDislikes", numDislikes));

      UpdateOneModel<Document> insertIfAbsentModel = new UpdateOneModel<>(filter, insertIfAbsent,
          new UpdateOptions().upsert(true));
      // This means that if no document matches the filter, no new document will be inserted.
      UpdateOneModel<Document> incUpdateModel = new UpdateOneModel<>(filter, incUpdate,
          new UpdateOptions().upsert(false));

      bulkOps.add(insertIfAbsentModel);
      bulkOps.add(incUpdateModel);
    }

    if (!bulkOps.isEmpty()) {
      collection.bulkWrite(bulkOps);
    }
  }
}