import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;

public class ConsumerRunnable implements Runnable{
  private final Connection connection;


  public ConsumerRunnable(Connection connection) {
    this.connection = connection;
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
//      channel.exchangeDeclare(Constant.EXCHANGE_NAME, "fanout"); // doesn't need in consumer
      channel.queueDeclare(Constant.QUEUE_NAME,true,false,false,null);
//      channel.queueBind(Constant.QUEUE_NAME, Constant.EXCHANGE_NAME, "");
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

      // Set up MongoDB URI
//    String uri = "mongodb://admin:admin@35.91.226.33/admin"; // ec2 public ip
      String uri = "mongodb://admin:admin@localhost:1234/?maxPoolSize=20&w=majority"; // ec2 public ip

      // Create MongoDB client
      MongoClientURI mongoClientURI = new MongoClientURI(uri);
      MongoClient mongoClient = new MongoClient(mongoClientURI);

      // Get MongoDB database
      MongoDatabase database = mongoClient.getDatabase("admin");

      // Update the MongoDB with swipe event
      Document swipeDocument = new Document("swiper", swiper)
          .append("swipee", swipee)
          .append("comment", comment)
          .append("isLike", isLike);
      MongoCollection<Document> collection = database.getCollection("swipeData");
      collection.insertOne(swipeDocument);

      // Close the connection
      mongoClient.close();

//      try {
//        SwipeRecord.addToLikeOrDislikeMap(swiper,isLike);
////        System.out.println(SwipeRecord.toNewString());
//      } catch (Exception e) {
//        throw new RuntimeException(e);
//      }
//
//      if(isLike) {
//        SwipeRecord.addToLikeMap(swiper, swipee,true);
//      }
      // ack the receipt and indicating the message can be removed from queue
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    try {
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      Logger.getLogger(ConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);;
    }
  }
}
