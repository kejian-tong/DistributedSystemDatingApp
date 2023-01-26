import com.google.gson.JsonObject;
import javax.servlet.http.HttpServletResponse;

public class ServletsUtil {

  public static void writeJsonObject(HttpServletResponse res, String resBody, JsonObject jsonObject) {
    try {
      jsonObject.addProperty("message", resBody);
      res.setContentType("application/json");
      res.getWriter().print(jsonObject.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



  public static boolean isValidNumber(String id) {
    if (id == null || id.isEmpty()) return false;
    try {
      int digits = Integer.parseInt(id);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }
}