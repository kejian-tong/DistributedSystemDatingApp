package assignment4.config.constant;

import com.mongodb.ReadConcernLevel;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class LoadTestConfig {
  //  public final static int CONSUMER_THREAD_NUM = 100;   // num of connections from Consumer to DB
//  // The actual DB connection from Consumer = min(CONSUMER_THREAD_NUM, CONSUMER_DB_MAX_CONNECTION)
  public final static int CONSUMER_DB_MAX_CONNECTION = 200;
  public static final int MATCHES_SERVLET_DB_MAX_CONNECTION = 80;
  public static final int STATS_SERVLET_DB_MAX_CONNECTION = 80;

  /**Consumer*/
  public static final int CONSUMER_BATCH_UPDATE_SIZE = 60;   // Consumer.
  public static final int CONSUMER_PROCESS_THREAD = 10;
  public static final Duration CONSUMER_POLL_TIMEOUT = Duration.of(100, ChronoUnit.MILLIS);
  public static final int CONSUMER_COMMIT_INTERVAL = 5000; // Unit: ms  ref: https://www.confluent.io/blog/kafka-consumer-multi-threaded-messaging/?_ga=2.148183543.135057811.1681237840-316597465.1681237839#:~:text=Committing%20offsets%20on,to%20five%20seconds.

  // Consumer DB BatchUpdate WriteConcern
  public static final int CONSUMER_DB_WRITE_CONCERN = 1; // W=R=1 or
  public static final int CONSUMER_DB_WRITE_TIMEOUT = 100;  // Unit: ms

  public static final ReadConcernLevel CONSUMER_DB_READ_CONCERN_LEVEL = ReadConcernLevel.MAJORITY;
  // local, available, linearizable
  // Read Concern level: https://www.mongodb.com/docs/manual/reference/read-concern/

  /**Producer*/
  public static final int PRODUCER_BATCH_SIZE = 3000; //3000; // 1638;//16384;    // unit: byte, default 16384

  public static final int PRODUCER_LINGER_MS = 1; // 1; // 0;//5; //unit:ms, default: 0

  public static final int PRODUCER_MAX_IN_FLIGHT = 5; // default:5


  /**Redis Cache*/
  public static final int REDIS_KEY_EXPIRATION_SECONDS = 60; // unit: second, freshness

  // CONSUMER_THREAD_NUM * CONSUMER_BATCH_UPDATE_SIZE must be divisble by 500K,
  // so that all msgs can be batch updated and ACK to RMQ??
}
