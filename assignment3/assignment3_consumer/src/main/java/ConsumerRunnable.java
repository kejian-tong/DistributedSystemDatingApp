import com.google.gson.Gson;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
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

public class ConsumerRunnable implements Runnable {

  private final Connection connection;
  private final MongoDatabase database;
  private final int batchSize = 100;
  private final int threadPoolSize = 30;
  private final AtomicInteger processedMessageCount = new AtomicInteger(0);

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
//    Timer timer = new Timer();
//    timer.schedule(new TimerTask() {
//      @Override
//      public void run() {
//        flushBatch(swipeBatch);
//      }
//    }, flushInterval, flushInterval);
//    System.out.println("Thread name is: " + Thread.currentThread().getName()); // TODO: to check if multiple threads are indeed being created and executed

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//      String threadName = Thread.currentThread().getName();
//      System.out.println("Received message on thread: " + threadName);

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

      // Check if the batch size reached 1000
      if (swipeBatch.size() >= batchSize) {
        flushBatch(swipeBatch);
        // ack the receipt and indicating the message can be removed from queue
//        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), true);
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
      ;
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

  // this commented function is a previous one early than the 5.30 am code
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

  // comment for this time 5:30 am Mar 29 2023, this commented can work but has some issues
//  private void processMatches(ConcurrentLinkedQueue<Document> batchToProcess) {
//    // Update user stats and insert matches in a loop
//    // Perform batch write to matches and stats collections
//    MongoCollection<Document> matchesCollection = database.getCollection("matches");
//    MongoCollection<Document> statsCollection = database.getCollection("stats");
//    List<WriteModel<Document>> matchesBulkOperations = new ArrayList<>();
//    List<WriteModel<Document>> statsBulkOperations = new ArrayList<>();
//    Map<Integer, Set<Integer>> mutualMatches = new HashMap<>();
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
//          mutualMatches.computeIfAbsent(swiper, k -> new HashSet<>()).add(swipee);
//        }
//      }
//    }
//
//    // Create bulk operations for the mutual matches
//    for (Map.Entry<Integer, Set<Integer>> entry : mutualMatches.entrySet()) {
//      Document existingMatchDocument = matchesCollection.find(
//          new Document("swiper", entry.getKey())).first();
//      if (existingMatchDocument != null) {
//        matchesCollection.updateOne(new Document("swiper", entry.getKey()),
//            new Document("$addToSet",
//                new Document("matchedSwipees", new Document("$each", entry.getValue()))));
//      } else {
//        Document matchDocument = new Document("swiper", entry.getKey())
//            .append("matchedSwipees", new ArrayList<>(entry.getValue()));
//        matchesBulkOperations.add(new InsertOneModel<>(matchDocument));
//      }
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
      Document query = new Document("_id", swiper);
      Document update;
      if (isLike) {
        update = new Document("$inc", new Document("numLikes", 1));
      } else {
        update = new Document("$inc", new Document("numDislikes", 1));
      }
      statsBulkOperations.add(new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));

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

    // Create bulk operations for the mutual matches and update the matches collection - commented on Mar39 11.38 am
//    for (Map.Entry<Integer, Set<Integer>> entry : mutualMatches.entrySet()) {
//      Document existingMatch = matchesCollection.find(new Document("swiper", entry.getKey())).first();
//      if (existingMatch != null) {
//        // Update existing document with new matches
//        Document updateMatch = new Document("$addToSet", new Document("matchedSwipees", new Document("$each", entry.getValue())));
//        matchesCollection.updateOne(new Document("_id", existingMatch.getObjectId("_id")), updateMatch);
//      } else {
//        // Insert a new document with the matches
//        Document matchDocument = new Document("swiper", entry.getKey())
//            .append("matchedSwipees", new ArrayList<>(entry.getValue()));
//        matchesBulkOperations.add(new InsertOneModel<>(matchDocument));
//      }
//    }
    for (Map.Entry<Integer, Set<Integer>> entry : mutualMatches.entrySet()) {
      Document query = new Document("_id", entry.getKey());
      Document update = new Document("$addToSet", new Document("matchedSwipees", new Document("$each", entry.getValue())));
      matchesBulkOperations.add(new UpdateOneModel<>(query, update, new UpdateOptions().upsert(true)));
    }

    if (!matchesBulkOperations.isEmpty()) {
      matchesCollection.bulkWrite(matchesBulkOperations);
    }
    if (!statsBulkOperations.isEmpty()) {
      statsCollection.bulkWrite(statsBulkOperations);
    }
  }
}