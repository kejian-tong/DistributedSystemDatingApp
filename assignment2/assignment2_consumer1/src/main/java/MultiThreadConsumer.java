import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.ConnectionFactory;


public class MultiThreadConsumer {
  private static Map<Integer, int[]> likeMap = new ConcurrentHashMap<>();
  private static Map<Integer, List<Integer>> listSwipeRight = new ConcurrentHashMap<>();


  public static void main (String[] args) throws IOException, TimeoutException {
    ConnectionFactory connectionFactory = new ConnectionFactory();
    connectionFactory.setHost(Constant.HOST_NAME);
    Connection connection = connectionFactory.newConnection();
    ExecutorService executorService = Executors.newFixedThreadPool(Constant.NUM_PER_THREADS);

    for(int i = 0; i < Constant.NUM_PER_THREADS; i++) {
      executorService.execute(new ConsumerRunnable(connection, likeMap, listSwipeRight));
    }
  }

}
