import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.ConnectionFactory;
import org.bson.Document;


public class MultiThreadConsumer {
  private static Integer NUM_PER_THREADS = Constant.NUM_PER_THREADS;


  public static void main (String[] args) throws IOException, TimeoutException {

//    CountDownLatch numberOfThreads = new CountDownLatch(20);
    // Set up MongoDB URI
    String uri = "mongodb://admin:admin@18.237.89.233:27017/?maxPoolSize=50"; // ec2 public ip

    // Create MongoDB client
    MongoClientURI mongoClientURI = new MongoClientURI(uri);
    MongoClient mongoClient = new MongoClient(mongoClientURI);

    // Get MongoDB database
    MongoDatabase database = mongoClient.getDatabase("admin");
    // Check if collections exist before creating them
    if (!collectionExists(database, "matches")) {
      database.createCollection("matches");
    }

    if (!collectionExists(database, "stats")) {
      database.createCollection("stats");
    }

    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(Constant.HOST_NAME);
    connectionFactory.setVirtualHost("cherry_broker"); // added ec2 RMQ vhost
    connectionFactory.setPort(5672);
//    connectionFactory.setUsername("guest");
//    connectionFactory.setPassword("guest");
    // for ec2 user, should be used the below one
    connectionFactory.setUsername("admin");
    connectionFactory.setPassword("admin");
    Connection connection = connectionFactory.newConnection();
    ExecutorService executorService = Executors.newFixedThreadPool(Constant.NUM_PER_THREADS);

    for(int i = 0; i < NUM_PER_THREADS; i++) {
      executorService.execute(new ConsumerRunnable(connection, database));
    }


    // Close the connection
    while (!executorService.isShutdown()) {
    }
    mongoClient.close();

  }

  public static boolean collectionExists(MongoDatabase database, String collectionName) {
    for (String name : database.listCollectionNames()) {
      if (name.equalsIgnoreCase(collectionName)) {
        return true;
      }
    }
    return false;
  }

  public static void insertMatch(MongoDatabase database, Integer swiper, Integer swipee) {
    Document matchDocument = new Document("swiper", swiper)
        .append("swipee", swipee);
    MongoCollection<Document> collection = database.getCollection("matches");
    collection.insertOne(matchDocument);
  }

  public static void updateUserStats(MongoDatabase database, Integer swiper, boolean isLike) {
    MongoCollection<Document> collection = database.getCollection("stats");
    Document query = new Document("swiper", swiper);
    Document update;
    if (isLike) {
      update = new Document("$inc", new Document("numLikes", 1));
    } else {
      update = new Document("$inc", new Document("numDislikes", 1));
    }
    collection.updateOne(query, update, new UpdateOptions().upsert(true));
  }

}
