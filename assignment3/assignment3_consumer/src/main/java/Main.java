import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Main {
  public static void main(String[] args) {
    // Set up MongoDB URI
//    String uri = "mongodb://admin:admin@35.91.226.33/admin"; // ec2 public ip
    String uri = "mongodb://admin:admin@localhost:1234/?maxPoolSize=20&w=majority";

    // Create MongoDB client
    MongoClientURI mongoClientURI = new MongoClientURI(uri);
    MongoClient mongoClient = new MongoClient(mongoClientURI);

    // Get MongoDB database
    MongoDatabase database = mongoClient.getDatabase("admin");

    Document swipeDocument = new Document("swiper", "swiper")
        .append("swipee", "swipee")
        .append("comment", "comment")
        .append("isLike", "isLike");
    MongoCollection<Document> collection = database.getCollection("swipeData");
    collection.insertOne(swipeDocument);
    // Do something with the database
    // ...

    // Close the connection
    mongoClient.close();
  }
}


