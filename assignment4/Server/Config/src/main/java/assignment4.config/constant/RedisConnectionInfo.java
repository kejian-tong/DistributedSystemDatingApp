package assignment4.config.constant;

public class RedisConnectionInfo {

  public static final String REDIS_URI = "redis://172.31.26.200:6379"; // private: "172.31.31.79:6379";

  public static final int POOL_MAX_TOTAL_CONN = 200; //default:8
  public static final int POOL_MAX_IDLE_CONN = 100;
}
