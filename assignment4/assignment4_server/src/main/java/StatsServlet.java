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
import java.util.NoSuchElementException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import com.mongodb.MongoClient;
import reactor.core.publisher.Mono;


@WebServlet(name = "StatsServlet", value = "/StatsServlet")
public class StatsServlet extends HttpServlet {
  private static MongoClient mongoClient;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // Redis related variables
  private RedisClient redisClient;
  private StatefulRedisConnection<String, String> connection;
  private RedisReactiveCommands<String, String> redisCommands;
  private final static int REDIS_KEY_EXPIRATION_SECONDS = 60; // 60 seconds


  @Override
  public void init() throws ServletException{
    super.init();
    try {
      String uri = "mongodb://admin:admin@35.86.112.85:27017/?maxPoolSize=150"; // ec2 mongodb public ip
      // Create MongoDB client
      if(mongoClient == null) {
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        mongoClient = new com.mongodb.MongoClient(mongoClientURI);
      }

    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for StatsServlet: " + me);
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
    MongoCollection<Document> statsCollection = database.getCollection("stats");

    fetchStats(statsCollection, swiperId, responseMsg, res);
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

  private void fetchStats(MongoCollection<Document> collection, Integer swiperId,
      ResponseMsg responseMsg, HttpServletResponse res)
      throws IOException {

    String redisKey = "stats:" + swiperId;
    redisCommands.get(redisKey)
        .switchIfEmpty(Mono.fromCallable(() -> { // fromCallable is better to handle exception than Mono.defer
          Document doc = collection.find(eq("_id", swiperId)).first(); // if not found in redis, find it in mongodb

          if (doc == null) {
            throw new NoSuchElementException("No document found with swiperId " + swiperId);
          } else {
            int likes = doc.getInteger("numLikes");
            int dislikes = doc.getInteger("numDislikes");
            Stats stats = new Stats().numLikes(likes).numDislikes(dislikes);
            String statsJson = gson.toJson(stats);

            redisCommands.set(redisKey, statsJson).subscribe(); // store json string in redis for future requests

            return statsJson;
          }
        }))
        .onErrorResume(throwable -> { // to handle errors
          if (throwable instanceof NoSuchElementException) {
            return AbstractGetMono.getMono(responseMsg, res, gson);
          } else {
            System.err.println("Error fetching stats: " + throwable);
            return Mono.error(throwable);
          }
        })
        .doOnSuccess(statsJson -> { // to process the response before sending it to the client
          responseMsg.setMessage(statsJson);
          res.setStatus(HttpServletResponse.SC_OK);
        })
        .subscribe(// subscribe to the Mono to initiate processing
            statsJson -> {
              try {
                res.getOutputStream().print(gson.toJson(responseMsg));
                res.getOutputStream().flush();
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            },
            throwable -> {
              res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        );
//    Document doc = collection.find(eq("_id", swiperId)).first();
//    if (doc == null) {
//      responseMsg.setMessage("User Not Found");
//      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
//      res.getOutputStream().print(gson.toJson(responseMsg));
//      res.getOutputStream().flush();
//      System.out.println("User Not Found:" + swiperId);
//    } else {
////      Object likesRes = doc.get("numLikes");
////      Object dislikesRes = doc.get("numDislikes");
//      int likes = doc.getInteger("numLikes");
//      int dislikes = doc.getInteger("numDislikes");
//      Stats stats = new Stats().numLikes(likes).numDislikes(dislikes);
////      Stats stats;
////      // handle error like 500
////      if (likesRes != null && dislikesRes !=null) {
////        stats = new Stats().numLikes((Integer) (likesRes)).numDislikes((Integer)(dislikesRes));
////      }
////      else if (likesRes!=null) {
////        stats = new Stats().numLikes((Integer) (likesRes)).numDislikes(0);
////      } else{
////          stats = new Stats().numLikes(0).numDislikes(0);
////        }
//
//      res.setStatus(HttpServletResponse.SC_OK);
//      res.getWriter().print(gson.toJson(stats));
//      res.getWriter().flush();
//    }
  }
}
