import static com.mongodb.client.model.Filters.eq;
import java.io.IOException;
import java.util.NoSuchElementException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@WebServlet(name = "StatsServlet", value = "/StatsServlet")
public class StatsServlet extends HttpServlet {
  private static MongoClient mongoClient;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // Redis related variables
  private JedisPool jedisPool;
  private final static int REDIS_KEY_EXPIRATION_SECONDS = 60; // 60 seconds

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      String uri = "mongodb://admin:admin@35.86.112.85:27017/?maxPoolSize=150"; // ec2 mongodb public ip
      // Create MongoDB client
      if (mongoClient == null) {
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        mongoClient = new MongoClient(mongoClientURI);
      }

    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for StatsServlet: " + me);
    }

    // Initialize Jedis pool
    JedisPoolConfig poolConfig = new JedisPoolConfig();
    poolConfig.setMaxTotal(200);
    poolConfig.setMaxIdle(100);
    jedisPool = new JedisPool(poolConfig, "ec2_ip_address"); // TODO: replace redis ec2 ip
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
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("application/json");
    ResponseMsg responseMsg = new ResponseMsg();

    Integer swiperId = validatePath(req, res, responseMsg);

    if (swiperId == null) {
      return;
    }

    MongoDatabase database = mongoClient.getDatabase("admin");
    MongoCollection<Document> statsCollection = database.getCollection("stats");

    fetchStats(statsCollection, swiperId, responseMsg, res);
  }

  private Integer validatePath(HttpServletRequest req, HttpServletResponse res, ResponseMsg responseMsg)
      throws IOException {
    String urlPath = req.getPathInfo();
    if (urlPath == null || urlPath.isEmpty()) {
      responseMsg.setMessage("Missing Parameter");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
      return null;
    }

    String[] urlParts = urlPath.split("/");
    if (!isValidNumber(urlParts[1])) {
      responseMsg.setMessage("Invalid url parameter: should be an ID");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
      return null;
    }

    return Integer.parseInt(urlParts[1]);
  }

  private boolean isValidNumber(String s) {
    if (s == null || s.isEmpty())
      return false;
    try {
      int digits = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  private void fetchStats(MongoCollection<Document> collection, Integer swiperId, ResponseMsg responseMsg,
      HttpServletResponse res) throws IOException {

    String redisKey = "stats:" + swiperId;
    try (Jedis jedis = jedisPool.getResource()) {
      String statsJson = jedis.get(redisKey);
      if (statsJson == null) {
        Document doc = collection.find(eq("_id", swiperId)).first();
        if (doc == null) {
          throw new NoSuchElementException("No document found with swiperId " + swiperId);
        } else {
          int likes = doc.getInteger("numLikes");
          int dislikes = doc.getInteger("numDislikes");
          Stats stats = new Stats().numLikes(likes).numDislikes(dislikes);
          statsJson = gson.toJson(stats);

          jedis.setex(redisKey, REDIS_KEY_EXPIRATION_SECONDS, statsJson); // store json string in redis for future requests
        }
      }

      responseMsg.setMessage(statsJson);
      res.setStatus(HttpServletResponse.SC_OK);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
    } catch (Exception e) {
      System.err.println("Error fetching stats: " + e);
      responseMsg.setMessage("Error fetching stats: " + e.getMessage());
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
    }
  }
}


