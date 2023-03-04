import RMQPool.RMQChannelFactory;
import RMQPool.RMQChannelPool;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.*;
import com.google.gson.*;
import javax.servlet.annotation.*;
import java.io.IOException;


@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {
  public final static String EXCHANGE_NAME = "swipe_exchange";
  private final static Integer POOL_SIZE = 30;
  private RMQChannelPool rmqChannelPool;
  private Gson gson = new GsonBuilder().setPrettyPrinting().create();

  public SwipeServlet() {
    try {
      ConnectionFactory factory = new ConnectionFactory();
//      factory.setHost("localhost");
      factory.setHost("34.208.62.124"); // ec2 RMQ, need to be updated every time and deploy to ec2
      factory.setVirtualHost("cherry_broker"); // added ec2 RMQ vhost
      factory.setPort(5672);
      factory.setUsername("admin");
      factory.setPassword("admin");
      final Connection conn = factory.newConnection();
      RMQChannelFactory channelFactory = new RMQChannelFactory(conn);
      rmqChannelPool = new RMQChannelPool(POOL_SIZE, channelFactory);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }
  }



  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    processHttpMethod(req, res, HttpMethod.POST);

    }

  private void processHttpMethod(HttpServletRequest req, HttpServletResponse res, HttpMethod method)
      throws IOException {
    res.setContentType("application/json");
    ResponseMsg responseMsg  = new ResponseMsg();
    Gson gson = new Gson();

    String urlPath = req.getPathInfo();

    // check if we have an url path
    if (urlPath == null || urlPath.isEmpty()) {
      responseMsg.setMessage("Missing Parameter");
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
      return;
    }

    String[] urlParts = urlPath.split("/");

    //check if the url is valid
    if (!isValidUrl(urlParts)) {
      responseMsg.setMessage("Invalid url parameter: should be left or right");
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getOutputStream().flush();
      return;
    }

    // swipe left or right, right=like
    String leftOrRight = urlParts[1];
    boolean isLike = leftOrRight.equals("right") ? true : false;

    try {
      StringBuilder sb = new StringBuilder();
      String s;
      while ((s = req.getReader().readLine()) != null) {
        sb.append(s);
      }

      // check if request json body is valid
      SwipeDetails swipeDetails = (SwipeDetails) gson.fromJson(sb.toString(), SwipeDetails.class);
      if(!validSwiper(swipeDetails.getSwiper())) {
        responseMsg.setMessage("User not found: invalid swiper id");
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!validSwipee(swipeDetails.getSwipee())) {
        responseMsg.setMessage("User not found: invalid swipee id");
        res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      } else if (!validComment(swipeDetails.getComment())) {
        responseMsg.setMessage("Invalid comments: comments can not exceed 256 characters");
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      } else {
        swipeDetails.setLike(isLike);
        sendMsgToQueue(swipeDetails);
        responseMsg.setMessage("Write successful");
        res.setStatus(HttpServletResponse.SC_CREATED);
      }
      res.getWriter().write(gson.toJson(responseMsg));
    } catch (Exception e) {
      e.printStackTrace();
      responseMsg.setMessage(e.getMessage());
    }
  }

  private boolean isValidUrl(String[] urlParts) {
    if ((urlParts[1].equals("left") && urlParts.length == 2) || (urlParts[1].equals("right") && urlParts.length == 2)) {
      return true;
    }
    return false;
  }

  private boolean isLike(String[] urlParts) {
    if(urlParts[1].equals("right") && urlParts.length == 2) {
      return true;
    }
    return false;
  }

  private boolean validSwiper(String swiper) {
    try {
      int swiperId = Integer.parseInt(swiper);
      if (swiperId < 1 || swiperId > 5000 || !isValidNumber(swiper)) {
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }


  private boolean validSwipee(String swipee) {
    try {
      int swipeeId = Integer.parseInt(swipee);
      if (swipeeId < 1 || swipeeId > 1000000 || !isValidNumber(swipee)) {
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean validComment(String comment) {
    if (comment.length() > 256) {
      return false;
    }
    return true;
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

  private void sendMsgToQueue(SwipeDetails swipeDetails) {

    try {
      String swipeMessage = gson.toJson(swipeDetails);
      Channel channel = rmqChannelPool.borrowObject();
      channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
//      channel.queueDeclare(QUEUE_NAME, true,false,false, null);
      channel.basicPublish(EXCHANGE_NAME, "", MessageProperties.PERSISTENT_TEXT_PLAIN, swipeMessage.getBytes(StandardCharsets.UTF_8));
      rmqChannelPool.returnObject(channel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


//  private String formatSwipeDetails(SwipeDetails swipeDetails) {
//    JsonObject message = new JsonObject();
//    message.addProperty("swiper", swipeDetails.getSwiper());
//    message.addProperty("swipee", swipeDetails.getSwipee());
//    if (swipeDetails.getComment() != null) {
//      message.addProperty("comment", swipeDetails.getComment());
//    }
//    return message.toString();
//  }

}
