import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import reactor.core.publisher.Mono;

public class AbstractGetMono {

  public static Mono<? extends String> getMono(ResponseMsg responseMsg, HttpServletResponse res,
      Gson gson) {
    responseMsg.setMessage("User Not Found");
    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    try {
      res.getOutputStream().print(gson.toJson(responseMsg));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    try {
      res.getOutputStream().flush();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return Mono.empty();
  }
}