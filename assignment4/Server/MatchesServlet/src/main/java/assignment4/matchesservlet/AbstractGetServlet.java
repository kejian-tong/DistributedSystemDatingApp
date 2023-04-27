package assignment4.matchesservlet;

import assignment4.config.datamodel.ResponseMsg;
import assignment4.config.datamodel.SwipeDetails;
import assignment4.config.util.Pair;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public abstract class AbstractGetServlet extends HttpServlet {
  protected Integer validateAndExtractId(HttpServletRequest request, HttpServletResponse response,
      ResponseMsg responseMsg, Gson gson)
      throws IOException {

    String urlPath = request.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      responseMsg.setMessage("missing path parameter: userID");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return null;
    }

    // check if URL path HAS exactly one parameter (userId) and
    // userId has a valid value: is integer and within range
    Pair urlValidationRes = this.isUrlValid(urlPath);
    if (!urlValidationRes.isUrlPathValid()) {
      responseMsg.setMessage("invalid path parameter: should be a positive integer <= " + Math.max(
          SwipeDetails.MAX_SWIPEE_ID, SwipeDetails.MAX_SWIPER_ID));
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // Invalid inputs
      response.getOutputStream().print(gson.toJson(responseMsg));
      response.getOutputStream().flush();
      return null;
    }

    Integer swiperId = Integer.valueOf(urlValidationRes.getParam());
    return swiperId;
  }


  private Pair isUrlValid(String urlPath) {
    /**
     * Check if url path has exactly one param: {userId} and its valid(within  range)
     */
    String[] urlParts = urlPath.split("/");

    if (urlParts.length != 2) {
      System.out.print("not 2");
      return new Pair(false, null);
    }

    int userId;
    try {
      userId = Integer.valueOf(urlParts[1]);
      return new Pair(userId <= SwipeDetails.MAX_SWIPER_ID && userId >= 1,
          urlParts[1]);
    } catch (NumberFormatException e) {
      return new Pair(false, null);
    }
  }
}


