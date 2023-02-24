import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsumerRunnable implements Runnable{
  Connection connection;

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

//      System.out.println(isLike);

//      System.out.println("received message " + message);
      try {
        SwipeRecord.addToLikeOrDislikeMap(swiper,isLike);
//        System.out.println(SwipeRecord.toNewString());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    try {
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      Logger.getLogger(ConsumerRunnable.class.getName()).log(Level.SEVERE, null, e);;
    }

  }
}
