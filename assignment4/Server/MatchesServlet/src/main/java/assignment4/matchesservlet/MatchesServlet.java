package assignment4.matchesservlet;

import static com.mongodb.client.model.Filters.eq;

import assignment4.config.constant.LoadTestConfig;
import assignment4.config.constant.MongoConnectionInfo;
import assignment4.config.constant.RedisConnectionInfo;
import assignment4.config.datamodel.Matches;
import assignment4.config.datamodel.ResponseMsg;
import com.google.gson.Gson;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@WebServlet(name = "assignment4.matchesservlet.MatchesServlet", value = "/matches")
public class MatchesServlet extends AbstractGetServlet {
  private MongoClient mongoClient;
  private final static Class<? extends List> listDocClazz = new ArrayList<Document>().getClass();
  private final static int MAX_MATCH_SIZE = 100;

  private JedisPool jedisPool;


  @Override
  public void init() throws ServletException {
    super.init();

    MongoClientSettings settings = MongoConnectionInfo.buildMongoSettingsForGet("Matches");
    try {
      this.mongoClient = MongoClients.create(settings);
    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for MatchesServlet: " + me);
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

    // fetch data from redis to check if there's a record based on cache key value
    String redisKey = "matches:" + swiperId;
    try (Jedis jedis = jedisPool.getResource()) {
      String cachedMatches = jedis.get(redisKey);

      if (cachedMatches != null) {  // cache hit
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print(cachedMatches);
        response.getWriter().flush();
        System.out.println("MatchesServlet Respond to Client: Fetched from cache for: " + swiperId);
      } else {    // cache miss
        // Connect to MongoDB
        MongoDatabase database = this.mongoClient.getDatabase(MongoConnectionInfo.DATABASE);
        MongoCollection<Document> matchesCollection = database.getCollection(
            MongoConnectionInfo.MATCH_COLLECTION);
        this.readMatchesCollection(matchesCollection, swiperId, gson, responseMsg, response,
            redisKey, jedis);
      }
    }
  }

  private void readMatchesCollection(MongoCollection<Document> collection, Integer swiperId,
      Gson gson, ResponseMsg responseMsg, HttpServletResponse response, String redisKey, Jedis jedis)
      throws IOException {
    Document doc = collection.find(eq("_id", swiperId)).first();

    if (doc == null) {  // <--> No document matches the _id
      responseMsg.setMessage("User Not Found");
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      System.out.println("MatchesServlet Respond to Client: User Not Found:" + swiperId);
    } else {
      List<Integer> matchesList = doc.get("matches", listDocClazz);
      if (matchesList.size() > MAX_MATCH_SIZE) {
        matchesList = matchesList.subList(0, MAX_MATCH_SIZE);
      }
      // Convert List<Integer> to List<String>
      Matches matches = new Matches(matchesList.stream().map(id -> String.valueOf(id)).collect(
          Collectors.toList()));
      String matchesJson = gson.toJson(matches);
      responseMsg.setMessage("Fetched Matches for userId " + swiperId + "!");
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().print(matchesJson);
      response.getWriter().flush();
      System.out.println("MatchesServlet Respond to Client: Fetched for:" + swiperId);

      // Cache the matches in Redis
      jedis.setex(redisKey, LoadTestConfig.REDIS_KEY_EXPIRATION_SECONDS, matchesJson);

    }
  }
}

