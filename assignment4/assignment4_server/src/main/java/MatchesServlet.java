import static com.mongodb.client.model.Filters.eq;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import com.mongodb.MongoClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@WebServlet(name = "MatchesServlet", value = "/MatchesServlet")
public class MatchesServlet extends HttpServlet {
  private MongoClient mongoClient;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final static int MAX_SIZE = 100; // returned 100 matches users
  // Redis related variables
  private JedisPool jedisPool;
  private final static int REDIS_KEY_EXPIRATION_SECONDS = 60; // 60 seconds

  @Override
  public void init() throws ServletException{
    super.init();
    try {
      String uri = "mongodb://admin:admin@35.86.112.85:27017/?maxPoolSize=150"; // TODO: ec2 mongodb public ip
      // Create MongoDB client
      if(mongoClient == null) {
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        mongoClient = new com.mongodb.MongoClient(mongoClientURI);
      }

    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for MatchesServlet: " + me);
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
    MongoCollection<Document> matchesCollection = database.getCollection("matches");

    fetchMatches(matchesCollection, swiperId, responseMsg, res);
  }

  private Integer validatePath(HttpServletRequest req, HttpServletResponse res,
      ResponseMsg responseMsg) throws IOException {
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
    if (s == null || s.isEmpty()) return false;
    try {
      int digits = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }


//  private void fetchMatches(MongoCollection<Document> collection, Integer swiperId,
//      ResponseMsg responseMsg, HttpServletResponse res)
//      throws IOException {
//    String redisKey = "user_matches:" + swiperId;
//
//    try (Jedis jedis = jedisPool.getResource()) {
//      String matchesJson = jedis.get(redisKey);
//
//      if (matchesJson == null) {
//        Document doc = collection.find(eq("_id", swiperId)).first();
//
//        if (doc == null) {
//          responseMsg.setMessage("User Not Found");
//          res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//          res.getOutputStream().print(gson.toJson(responseMsg));
//          res.getOutputStream().flush();
//        } else {
//          List<String> matchesList = doc.getList("matchedSwipees", Integer.class).stream()
//              .map(String::valueOf).collect(Collectors.toList());
//
//          // Limit the size of the matchesList after fetching from MongoDB and before storing in Redis
//          if (matchesList.size() > MAX_SIZE) {
//            matchesList = matchesList.subList(0, MAX_SIZE);
//          }
//
//          Matches matches = new Matches(matchesList);
//          matchesJson = gson.toJson(matches);
//          jedis.setex(redisKey, REDIS_KEY_EXPIRATION_SECONDS, matchesJson);
//          sendMatchesResponse(matches, swiperId, responseMsg, res);
//        }
//      } else {
//        Matches matches = gson.fromJson(matchesJson, Matches.class);
//        sendMatchesResponse(matches, swiperId, responseMsg, res);
//      }
//    } catch (Exception e) {
//      System.err.println("Error fetching matches from Redis: " + e);
//      // Fall back to fetching from MongoDB
//      Document doc = collection.find(eq("_id", swiperId)).first();
//
//      if (doc == null) {
//        responseMsg.setMessage("User Not Found");
//        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        res.getOutputStream().print(gson.toJson(responseMsg));
//        res.getOutputStream().flush();
//      } else {
//        List<String> matchesList = doc.getList("matchedSwipees", Integer.class).stream()
//            .map(String::valueOf).collect(Collectors.toList());
//
//        // Limit the size of the matchesList after fetching from MongoDB and before storing in Redis
//        if (matchesList.size() > MAX_SIZE) {
//          matchesList = matchesList.subList(0, MAX_SIZE);
//        }
//
//        Matches matches = new Matches(matchesList);
//        responseMsg.setMessage("Get user matches successfully: " + swiperId);
//        res.setStatus(HttpServletResponse.SC_OK);
//        res.getWriter().print(gson.toJson(matches));
//        res.getWriter().flush();
//      }
//    }
//  }

  private void fetchMatches(MongoCollection<Document> collection, Integer swiperId,
      ResponseMsg responseMsg, HttpServletResponse res)
      throws IOException {
    String redisKey = "user_matches:" + swiperId;

    try (Jedis jedis = jedisPool.getResource()) {
      String matchesJson = jedis.get(redisKey);

      if (matchesJson == null) {
        Document doc = collection.find(eq("_id", swiperId)).first();
        if (doc == null) {
          sendNotFoundResponse("User Not Found", responseMsg, res);
          return;
        }
        matchesJson = getMatchesJsonFromMongo(doc);
        // Store the matches JSON in Redis for future requests (used for caching)
        jedis.setex(redisKey, REDIS_KEY_EXPIRATION_SECONDS, matchesJson);
      }

      Matches matches = gson.fromJson(matchesJson, Matches.class);
      sendMatchesResponse(matches, swiperId, responseMsg, res);
    } catch (Exception e) {
      System.err.println("Error fetching matches from Redis: " + e);
      fallbackToMongo(collection, swiperId, responseMsg, res);
    }
  }

  private String getMatchesJsonFromMongo(Document doc) {
    List<String> matchesList = doc.getList("matchedSwipees", Integer.class).stream()
        .map(String::valueOf).collect(Collectors.toList());

    if (matchesList.size() > MAX_SIZE) {
      matchesList = matchesList.subList(0, MAX_SIZE);
    }

    Matches matches = new Matches(matchesList);
    return gson.toJson(matches);
  }

  private void fallbackToMongo(MongoCollection<Document> collection, Integer swiperId, ResponseMsg responseMsg,
      HttpServletResponse res) throws IOException {
    Document doc = collection.find(eq("_id", swiperId)).first();

    if (doc == null) {
      sendNotFoundResponse("User Not Found", responseMsg, res);
    } else {
      String matchesJson = getMatchesJsonFromMongo(doc);
      Matches matches = gson.fromJson(matchesJson, Matches.class);
      sendMatchesResponse(matches, swiperId, responseMsg, res);
    }
  }

  private void sendNotFoundResponse(String message, ResponseMsg responseMsg, HttpServletResponse res) throws IOException {
    responseMsg.setMessage(message);
    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    res.getOutputStream().print(gson.toJson(responseMsg));
    res.getOutputStream().flush();
  }

  // send matches responses to the client after successfully fetch matches data
  private void sendMatchesResponse(Matches matches, Integer swiperId, ResponseMsg responseMsg, HttpServletResponse res)
      throws IOException {
    responseMsg.setMessage("Get user matches successfully: " + swiperId);
    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().print(gson.toJson(matches));
    res.getWriter().flush();
  }
}

