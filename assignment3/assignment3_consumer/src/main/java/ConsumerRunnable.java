import com.google.gson.Gson;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

public class ConsumerRunnable implements Runnable{
  private final Connection connection;
  private final MongoDatabase database;
  private final int batchSize = 1000;
  private final long flushInterval = 10000; // 10 seconds
  private final int threadPoolSize = 10;

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
      channel.queueDeclare(Constant.QUEUE_NAME,true,false,false,null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    List<Document> swipeBatch = new ArrayList<>();

    // Timer to periodically flush remaining events in the batch
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
      }

      // ack the receipt and indicating the message can be removed from queue
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    try {
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      Logger.getLogger(ConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);;
    }
  }

  public synchronized void flushBatch(List<Document> swipeBatch) {
    if (!swipeBatch.isEmpty()) {
      List<Document> batchToProcess = new ArrayList<>(swipeBatch);
      swipeBatch.clear();

      // Submit the task to the thread pool
      executor.submit(() -> {
        // Perform batch write
        MongoCollection<Document> collection = database.getCollection("swipeData");
        List<WriteModel<Document>> bulkOperations = new ArrayList<>();
        for (Document doc : batchToProcess) {
          bulkOperations.add(new InsertOneModel<>(doc));
        }
        BulkWriteResult result = collection.bulkWrite(bulkOperations);

        // Update user stats and insert matches in a loop
        for (Document doc : batchToProcess) {
          int swiper = doc.getInteger("swiper");
          int swipee = doc.getInteger("swipee");
          boolean isLike = doc.getBoolean("isLike");

          // Update user stats
          MultiThreadConsumer.updateUserStats(database, swiper, isLike);

          // Check for a match and insert it if found
          if (isLike) {
            Set<Integer> swipeRightSet = SwipeRecord.listSwipeRight.get(swipee);
            if (swipeRightSet != null && swipeRightSet.contains(swiper)) {
              MultiThreadConsumer.insertMatch(database, swiper, swipee);
            }
            SwipeRecord.addToLikeMap(swiper, swipee, true);
          }
        }
      });
    }
  }
}

