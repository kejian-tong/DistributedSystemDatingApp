import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

public class ConsumerRunnable implements Runnable {

  private final Connection connection;
  private final MongoDatabase database;
  private final int batchSize = 100;
  private final long flushInterval = 10000; // 10 seconds
  private final int threadPoolSize = 30;

  private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
      threadPoolSize, // corePoolSize
      threadPoolSize, // maximumPoolSize
      0L,
      TimeUnit.MILLISECONDS,
      new LinkedBlockingQueue<>()
  );

  //  private final Queue<Document> swipeBatch = new ConcurrentLinkedQueue<>();
  private final List<Document> swipeBatch = new ArrayList<>();

  public ConsumerRunnable(Connection connection, MongoDatabase database) {
    this.connection = connection;
    this.database = database;
  }

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

//    // Timer to periodically flush remaining events in the batch
    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        flushBatch(swipeBatch);
      }
    }, flushInterval, flushInterval);

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

      // Check if the batch size reached 1000
      if (swipeBatch.size() >= batchSize) {
        flushBatch(swipeBatch);
        // ack the receipt and indicating the message can be removed from queue
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
      }
    };

    try {// comment for basicConsume which is used to consume the message from the queue
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      Logger.getLogger(ConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);
      ;
    }
  }

  public void flushBatch(List<Document> swipeBatch) {
    if (!swipeBatch.isEmpty()) {
      ConcurrentLinkedQueue<Document> batchToProcess = new ConcurrentLinkedQueue<>(swipeBatch);
      swipeBatch.clear();

      // Submit the task to the thread pool
      executor.submit(() -> processMatches(batchToProcess));
      // Submit the task to the thread pool
//      executor.submit(() -> {
//        // Update user stats and insert matches in a loop
//        // Perform batch write to matches and stats collections
//        MongoCollection<Document> matchesCollection = database.getCollection("matches");
//        MongoCollection<Document> statsCollection = database.getCollection("stats");
//        List<WriteModel<Document>> matchesBulkOperations = new ArrayList<>();
//        List<WriteModel<Document>> statsBulkOperations = new ArrayList<>();
//
//        for (Document doc : batchToProcess) {
//          int swiper = doc.getInteger("swiper");
//          int swipee = doc.getInteger("swipee");
//          boolean isLike = doc.getBoolean("isLike");
//
//          // Update user stats
//          Document query = new Document("swiper", swiper);
//          Document update;
//          if (isLike) {
//            update = new Document("$inc", new Document("numLikes", 1));
//          } else {
//            update = new Document("$inc", new Document("numDislikes", 1));
//          }
//          statsBulkOperations.add(new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));
//
//          // Check for a match and insert it if found
////          if (isLike) {
////            SwipeRecord.addToLikeMap(swiper, swipee, true);
////            Set<Integer> swipeeRightSet = SwipeRecord.listSwipeRight.get(swipee);
////            if (swipeeRightSet != null && swipeeRightSet.contains(swiper)) {
////              Document matchDocument = new Document("swiper", swiper)
////                  .append("swipee", swipee);
////              matchesBulkOperations.add(new InsertOneModel<>(matchDocument));
////            }
////          }
//
//        }
//        if (!matchesBulkOperations.isEmpty()) {
//          matchesCollection.bulkWrite(matchesBulkOperations);
//        }
//        if (!statsBulkOperations.isEmpty()) {
//          statsCollection.bulkWrite(statsBulkOperations);
//        }
//      });
    }
  }

  //  private void processMatches(ConcurrentLinkedQueue<Document> batchToProcess) {
//    // Update user stats and insert matches in a loop
//    // Perform batch write to matches and stats collections
//    MongoCollection<Document> matchesCollection = database.getCollection("matches");
//    MongoCollection<Document> statsCollection = database.getCollection("stats");
//    List<WriteModel<Document>> matchesBulkOperations = new ArrayList<>();
//    List<WriteModel<Document>> statsBulkOperations = new ArrayList<>();
//    Map<Integer, List<Integer>> mutualMatches = new HashMap<>();
//
//    for (Document doc : batchToProcess) {
//      int swiper = doc.getInteger("swiper");
//      int swipee = doc.getInteger("swipee");
//      boolean isLike = doc.getBoolean("isLike");
//
//      // Update user stats
//      Document query = new Document("swiper", swiper);
//      Document update;
//      if (isLike) {
//        update = new Document("$inc", new Document("numLikes", 1));
//      } else {
//        update = new Document("$inc", new Document("numDislikes", 1));
//      }
//      statsBulkOperations.add(
//          new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));
//
//      // Check for a mutual match and insert it if found
//      if (isLike && swiper != swipee) {
//        SwipeRecord.addToLikeMap(swiper, swipee, true);
//        Set<Integer> swiperRightSet = SwipeRecord.listSwipeRight.get(swiper);
//        Set<Integer> swipeeRightSet = SwipeRecord.listSwipeRight.get(swipee);
//        if (swiperRightSet != null && swipeeRightSet != null && swiperRightSet.contains(swipee)
//            && swipeeRightSet.contains(swiper)) {
//          mutualMatches.computeIfAbsent(swiper, k -> new ArrayList<>()).add(swipee);
//        }
//      }
//    }
//
//    // Combine mutual matches with the same swiper ID
//    Map<Integer, List<Integer>> combinedMutualMatches = new HashMap<>();
//    for (Map.Entry<Integer, List<Integer>> entry : mutualMatches.entrySet()) {
//      combinedMutualMatches.merge(entry.getKey(), entry.getValue(), (oldList, newList) -> {
//        oldList.addAll(newList);
//        return oldList;
//      });
//    }
//
//    // Create bulk operations for the mutual matches
//    for (Map.Entry<Integer, List<Integer>> entry : mutualMatches.entrySet()) {
//      Document matchDocument = new Document("swiper", entry.getKey())
//          .append("matchedSwipees", entry.getValue());
//      matchesBulkOperations.add(new InsertOneModel<>(matchDocument));
//    }
//
//    if (!matchesBulkOperations.isEmpty()) {
//      matchesCollection.bulkWrite(matchesBulkOperations);
//    }
//    if (!statsBulkOperations.isEmpty()) {
//      statsCollection.bulkWrite(statsBulkOperations);
//    }
//  }
  private void processMatches(ConcurrentLinkedQueue<Document> batchToProcess) {
    // Update user stats and insert matches in a loop
    // Perform batch write to matches and stats collections
    MongoCollection<Document> matchesCollection = database.getCollection("matches");
    MongoCollection<Document> statsCollection = database.getCollection("stats");
    List<WriteModel<Document>> matchesBulkOperations = new ArrayList<>();
    List<WriteModel<Document>> statsBulkOperations = new ArrayList<>();
    Map<Integer, Set<Integer>> mutualMatches = new HashMap<>();

    for (Document doc : batchToProcess) {
      int swiper = doc.getInteger("swiper");
      int swipee = doc.getInteger("swipee");
      boolean isLike = doc.getBoolean("isLike");

      // Update user stats
      Document query = new Document("swiper", swiper);
      Document update;
      if (isLike) {
        update = new Document("$inc", new Document("numLikes", 1));
      } else {
        update = new Document("$inc", new Document("numDislikes", 1));
      }
      statsBulkOperations.add(
          new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));

      // Check for a mutual match and insert it if found
      if (isLike && swiper != swipee) {
        SwipeRecord.addToLikeMap(swiper, swipee, true);
        Set<Integer> swiperRightSet = SwipeRecord.listSwipeRight.get(swiper);
        Set<Integer> swipeeRightSet = SwipeRecord.listSwipeRight.get(swipee);
        if (swiperRightSet != null && swipeeRightSet != null && swiperRightSet.contains(swipee)
            && swipeeRightSet.contains(swiper)) {
          mutualMatches.computeIfAbsent(swiper, k -> new HashSet<>()).add(swipee);
        }
      }
    }

    // Create bulk operations for the mutual matches
    for (Map.Entry<Integer, Set<Integer>> entry : mutualMatches.entrySet()) {
      Document existingMatchDocument = matchesCollection.find(
          new Document("swiper", entry.getKey())).first();
      if (existingMatchDocument != null) {
        matchesCollection.updateOne(new Document("swiper", entry.getKey()),
            new Document("$addToSet",
                new Document("matchedSwipees", new Document("$each", entry.getValue()))));
      } else {
        Document matchDocument = new Document("swiper", entry.getKey())
            .append("matchedSwipees", new ArrayList<>(entry.getValue()));
        matchesBulkOperations.add(new InsertOneModel<>(matchDocument));
      }
    }

    if (!matchesBulkOperations.isEmpty()) {
      matchesCollection.bulkWrite(matchesBulkOperations);
    }
    if (!statsBulkOperations.isEmpty()) {
      statsCollection.bulkWrite(statsBulkOperations);
    }
  }

}