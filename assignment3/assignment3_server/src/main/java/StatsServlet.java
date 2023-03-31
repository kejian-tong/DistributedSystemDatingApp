import static com.mongodb.client.model.Filters.eq;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.bson.Document;
import com.mongodb.MongoClient;


@WebServlet(name = "StatsServlet", value = "/StatsServlet")
public class StatsServlet extends HttpServlet {
  private static MongoClient mongoClient;
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  @Override
  public void init() throws ServletException{
    super.init();
    try {
      String uri = "mongodb://admin:admin@35.87.116.245:27017/?maxPoolSize=50"; // ec2 mongodb public ip
      // Create MongoDB client
      if(mongoClient == null) {
        MongoClientURI mongoClientURI = new MongoClientURI(uri);
        mongoClient = new com.mongodb.MongoClient(mongoClientURI);
      }

    } catch (MongoException me) {
      System.err.println("Cannot create MongoClient for StatsServlet: " + me);
    }
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
    Document doc = collection.find(eq("swiper", swiperId)).first();

    if (doc == null) {
      responseMsg.setMessage("User Not Found");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
      System.out.println("User Not Found:" + swiperId);
    } else {
      int likes = doc.getInteger("numLikes");
      int dislikes = doc.getInteger("numDislikes");
      Stats stats = new Stats().numLikes(likes).numDislikes(dislikes);
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().print(gson.toJson(stats));
      res.getWriter().flush();
    }
  }
}
