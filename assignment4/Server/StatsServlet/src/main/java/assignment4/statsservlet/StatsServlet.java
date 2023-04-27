package assignment4.statsservlet;

import static com.mongodb.client.model.Filters.eq;
import assignment4.config.constant.MongoConnectionInfo;
import assignment4.config.constant.RedisConnectionInfo;
import assignment4.config.datamodel.MatchStats;
import assignment4.config.datamodel.ResponseMsg;
import com.google.gson.Gson;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@WebServlet(name = "assignment4.statsservlet.StatsServlet", value = "/stats")
public class StatsServlet extends AbstractGetServlet {
  private MongoClient mongoClient;
  private JedisPool jedisPool;
  private final static int REDIS_KEY_EXPIRATION_SECONDS = 60; // 60 second

  @Override
  public void init() throws ServletException {
    super.init();
    MongoClientSettings settings = MongoConnectionInfo.buildMongoSettingsForGet("Stats");
    try {
      this.mongoClient = MongoClients.create(settings);
    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for StatsServlet: " + me);
    }

    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(RedisConnectionInfo.POOL_MAX_TOTAL_CONN);
    poolConfig.setMaxIdle(RedisConnectionInfo.POOL_MAX_IDLE_CONN);
    jedisPool = new JedisPool(poolConfig, RedisConnectionInfo.REDIS_URI);
  }

  @Override
  public void destroy() {
    jedisPool.close(); // release any resources associated with the pool
    if (mongoClient != null) {
      mongoClient.close();
    }
    super.destroy();
  }


  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    ResponseMsg responseMsg = new ResponseMsg();
    Gson gson = new Gson();
    Integer swiperId = this.validateAndExtractId(request, response, responseMsg, gson);

    if (swiperId == null) {
      return;
    }

    String redisKey = "stats:" + swiperId;
    try (Jedis jedis = jedisPool.getResource()) {
      String statsJson = jedis.get(redisKey);
      if (statsJson != null) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(statsJson);
        response.getWriter().flush();
        System.out.println("StatsServlet Respond to Client (from cache): Fetched for:" + swiperId);
      } else {
        // Connect to MongoDB
        MongoDatabase database = this.mongoClient.getDatabase(MongoConnectionInfo.DATABASE);
        MongoCollection<Document> statsCollection = database.getCollection(MongoConnectionInfo.STATS_COLLECTION);
        this.readStatsCollection(statsCollection, swiperId, gson, responseMsg, response, jedis, redisKey);
      }
    }
  }

  private void readStatsCollection(MongoCollection<Document> collection, Integer swiperId, Gson gson,
      ResponseMsg responseMsg, HttpServletResponse response, Jedis jedis, String redisKey)
      throws IOException {
    Document doc = collection.find(eq("_id", swiperId)).first();

    if (doc == null) {  // <--> No document matches the _id
      responseMsg.setMessage("User Not Found");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      System.out.println("MatchesServlet Respond to Client: User Not Found:" + swiperId);
    } else {
      int likes = doc.getInteger("likes");
      int dislikes = doc.getInteger("dislikes");
      MatchStats stats = new MatchStats().numLikes(likes).numDislikes(dislikes);
      String statsJson = gson.toJson(stats);

      // Cache the stats in Redis
      jedis.setex(redisKey, REDIS_KEY_EXPIRATION_SECONDS, statsJson);

      responseMsg.setMessage("Fetched Stats for userId " + swiperId + "!");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().print(statsJson);
      response.getWriter().flush();
      System.out.println("StatsServlet Respond to Client: Fetched for:" + swiperId);
    }
  }
}
