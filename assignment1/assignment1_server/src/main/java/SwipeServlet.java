import javax.servlet.http.*;
import com.google.gson.*;
import javax.servlet.annotation.*;
import java.io.IOException;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {

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
//      } else if (method == HttpMethod.POST){
      } else {
        responseMsg.setMessage("Write successful");
        res.setStatus(HttpServletResponse.SC_CREATED);
      }
//      res.getOutputStream().print(gson.toJson(responseMsg));
      res.getWriter().write(gson.toJson(responseMsg));
//      res.getOutputStream().flush();

    } catch (Exception e) {
      e.printStackTrace();
      responseMsg.setMessage(e.getMessage());
    }
//    finally {
//      res.getOutputStream().print(gson.toJson(responseMsg));
//      res.getOutputStream().flush();
//    }
  }

  private boolean isValidUrl(String[] urlParts) {
    if ((urlParts[1].equals("left") && urlParts.length == 2) || (urlParts[1].equals("right") && urlParts.length == 2)) {
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
}
