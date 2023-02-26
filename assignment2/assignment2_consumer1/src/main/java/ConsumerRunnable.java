import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsumerRunnable implements Runnable{
  private final Connection connection;
  private static final int MAX_FAILURES = 3;
  private static final Duration CIRCUIT_OPEN_DURATION = Duration.ofMinutes(1);


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
      channel.queueBind(Constant.QUEUE_NAME, Constant.EXCHANGE_NAME, "");
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

      try {
        SwipeRecord.addToLikeOrDislikeMap(swiper,isLike);
//        System.out.println(SwipeRecord.toNewString());
      } catch (Exception e) {
        throw new RuntimeException(e);
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
}
