import static com.mongodb.client.model.Filters.eq;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
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
import reactor.core.publisher.Mono;


@WebServlet(name = "MatchesServlet", value = "/MatchesServlet")
public class MatchesServlet extends HttpServlet {
  private MongoClient mongoClient;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final static int MAX_SIZE = 100; // returned 100 matches users
  private final static int REDIS_KEY_EXPIRATION_SECONDS = 60; // 60 seconds

  // added redis part
  private RedisClient redisClient;
  private StatefulRedisConnection<String, String> connection;
  private RedisReactiveCommands<String, String> redisCommands;

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

    // Initialize RedisClient, connection, and reactive commands
    redisClient = RedisClient.create("redis://ec2_ip_address"); // TODO: replace redis ec2 ip
    connection = redisClient.connect();
    redisCommands = connection.reactive();
  }

  @Override
  public void destroy() {
    connection.close();
    redisClient.shutdown(); // release any resources associated with the client
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

  private void fetchMatches(MongoCollection<Document> collection, Integer swiperId,
      ResponseMsg responseMsg, HttpServletResponse res)
      throws IOException {
//    Document doc = collection.find(eq("_id", swiperId)).first();
    String redisKey = "user_matches:" + swiperId;
    redisCommands.get(redisKey)
        .switchIfEmpty(Mono.defer(
            () -> { // if key not found in redis, create a deferred calc that fetch data from mongodb
              Document doc = collection.find(eq("_id", swiperId)).first();

              if (doc == null) {
                return AbstractGetMono.getMono(responseMsg, res, gson);
              } else {
                List<String> matchesList = doc.getList("matchedSwipees", Integer.class).stream()
                    .map(String::valueOf).collect(
                        Collectors.toList());

                // Limit the size of the matchesList after fetching from MongoDB and before storing in Redis
                if (matchesList.size() > MAX_SIZE) {
                  matchesList = matchesList.subList(0, MAX_SIZE);
                }
                Matches matches = new Matches(matchesList);
                String matchesJson = gson.toJson(matches);

                return redisCommands.set(redisKey, matchesJson) // store json string in redis cache
                    .then(redisCommands.expire(redisKey, REDIS_KEY_EXPIRATION_SECONDS))
                    .thenReturn(matchesJson);
              }
            }))
        .doOnNext(matchesJson -> { // when the data is available, use `doOnNext` to perform data
          Matches matches = gson.fromJson(matchesJson, Matches.class);
          try {
            sendMatchesResponse(matches, swiperId, responseMsg, res);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .onErrorResume(e -> { // Handle any errors that occur during processing
          System.err.println("Error fetching matches: " + e);
          res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          return Mono.empty();
        })
        .block(); // block the thread until the result is returned
  }

//    if (doc == null) {
//      responseMsg.setMessage("User Not Found");
//      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//      res.getOutputStream().print(gson.toJson(responseMsg));
//      res.getOutputStream().flush();
////      System.out.println("User Not Found:" + swiperId);
//    } else {
//      // create a stream of matchedSwipees and convert to string list and collect the resulting String objects into a list
//      List<String> matchesList = doc.getList("matchedSwipees", Integer.class).stream().map(String::valueOf).collect(
//          Collectors.toList());
//      if(matchesList.size() > MAX_SIZE) {
//        matchesList = matchesList.subList(0, MAX_SIZE);
//      }
//      Matches matches = new Matches(matchesList);
//      responseMsg.setMessage("get user matches successfully" + swiperId);
//      res.setStatus(HttpServletResponse.SC_OK);
//      res.getWriter().print(gson.toJson(matches));
//      res.getWriter().flush();

  // send matches responses to the client after successfully fetch matches data
  private void sendMatchesResponse(Matches matches, Integer swiperId, ResponseMsg responseMsg, HttpServletResponse res)
      throws IOException {
    responseMsg.setMessage("Get user matches successfully: " + swiperId);
    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().print(gson.toJson(matches));
    res.getWriter().flush();
  }
}

