import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.ConnectionFactory;


public class MultiThreadConsumer {
//  private static Map<Integer, int[]> likeOrDislikeMap = new ConcurrentHashMap<>();
//  private static Map<Integer, List<Integer>> listSwipeRight = new ConcurrentHashMap<>();
  private static Integer NUM_PER_THREADS = Constant.NUM_PER_THREADS;


  public static void main (String[] args) throws IOException, TimeoutException {

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
      executorService.execute(new ConsumerRunnable(connection));
    }

  }
}
