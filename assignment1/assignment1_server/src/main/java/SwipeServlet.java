import javax.servlet.http.*;
import com.google.gson.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {
  private ServletsUtil servletsUtil = new ServletsUtil();


  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
    processHttpMethod(req, res, HttpMethod.POST);
  }

  private void processHttpMethod(HttpServletRequest req, HttpServletResponse res, HttpMethod method)
      throws IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("Missing Parameter");
      return;
    }

    String[] urlParts = urlPath.split("/");

    //check if the url is valid and json body is valid
    String jsonBodyString = servletsUtil.getJsonBodyString(req);
    if (!isValidUrl(urlParts) || !isJsonBodyValid(jsonBodyString, res)) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//      res.getWriter().write(urlPath);
      res.getWriter().write("Invalid url or json body");
      return;
    }

    if (method == HttpMethod.POST) {
      res.setStatus(HttpServletResponse.SC_CREATED);
      JsonObject jsonBody = new JsonParser().parse(jsonBodyString).getAsJsonObject();
      int swiper = jsonBody.get("swiper").getAsInt();
      int swipee = jsonBody.get("swipee").getAsInt();
      String comment = jsonBody.get("comment").getAsString();
      String resBody = "Write successful";

      JsonObject jsonObject = new JsonObject();
      servletsUtil.writeJsonObject(res, resBody, jsonObject);
    }
  }

  private boolean isValidUrl(String[] urlParts) {
    if (urlParts[1].equals("left") || urlParts[1].equals("right") ) {
      return true;
    }
    return false;
  }

  private boolean isJsonBodyValid(String jsonBodyString, HttpServletResponse res)
      throws IOException {
    try {
      JsonObject jsonBody = new JsonParser().parse(jsonBodyString).getAsJsonObject();
      // check if swiper is a number between 1 and 5000
      String swipers = jsonBody.get("swiper").getAsString();

      if(!isValidNumber(swipers)) {
        res.getWriter().write("Not valid number\n");
        return false;
      }
      int swiper = Integer.parseInt(swipers);
      if (swiper < 1 || swiper > 5000) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Invalid swiper value. Must be a number between 1 and 5000.");
        return false;
      }

      // check if swipee is a number between 1 and 1000000
      String swipees = jsonBody.get("swipee").getAsString();
      if(!isValidNumber(swipees)) {
        res.getWriter().write("Not a valid number\n");
        return false;
      }
      int swipee = Integer.parseInt(swipees);
      if (swipee < 1 || swipee > 1000000) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Invalid swipee value. Must be a number between 1 and 1000000.");
        return false;
      }

      // check if comment is a string of 256 characters or less
      String comment = jsonBody.get("comment").getAsString();
      if (comment.length() > 256) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Invalid comment value. Must be a string of 256 characters or less.");
        return false;
      }

    } catch (JsonSyntaxException e) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write("Error parsing json body.");
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

}
