import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletsUtil {

  public static void writeJsonObject(
      HttpServletResponse res, String resBody, JsonObject jsonObject) {
    try {
      jsonObject.addProperty("message", resBody);
      res.setContentType("application/json");
      res.getWriter().print(jsonObject.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static String getJsonBodyString(HttpServletRequest req) throws IOException {
    StringBuilder buffer = new StringBuilder();
    BufferedReader reader = req.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
      buffer.append(line);
    }
    return buffer.toString();
  }
}