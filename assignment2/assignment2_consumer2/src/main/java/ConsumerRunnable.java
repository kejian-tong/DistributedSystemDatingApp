import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ConsumerRunnable implements Runnable{
  Connection connection;
  Map<Integer, int[]> likeMap;
  Map<Integer, List<Integer>> listSwipeRight;

  public ConsumerRunnable(Connection connection, Map<Integer, int[]> likeMap,
      Map<Integer, List<Integer>> listSwipeRight) {
    this.connection = connection;
    this.likeMap = likeMap;
    this.listSwipeRight = listSwipeRight;
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

      System.out.println("rec msg " + message);
      try {
        SwipeRecord.addToNumOfLikeMap(swiper,isLike, likeMap);
        if(isLike) {
          SwipeRecord.addToNumOfLikeMap(swiper, swipee, listSwipeRight);
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    try {
      channel.basicConsume(Constant.QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
}
