import java.io.BufferedReader;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import com.google.gson.*;

@WebServlet(name = "SwipeServlet", value = "/SwipeServlet")
public class SwipeServlet extends HttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
        res.setContentType("application/json");
        Gson gson = new Gson();

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = req.getReader().readLine()) != null) {
                sb.append(s);
            }

            RequestBody requestBody = (RequestBody) gson.fromJson(sb.toString(), RequestBody.class);

            RequestBody reqBody = new RequestBody();
            if (isJsonBodyValid(sb.toString(), res)) {
                reqBody.setSwiper(1);
            } else {

            }
            res.getOutputStream().print(gson.toJson(requestBody));
            res.getOutputStream().flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
            res.setContentType("text/plain");

            // post portion
            BufferedReader bufferedReaderBody = req.getReader();
            StringBuilder jsonBodyBuilder = new StringBuilder();

            String line;
            while((line = bufferedReaderBody.readLine()) != null) {
                jsonBodyBuilder.append(line);
            }
            String jsonBodyString = jsonBodyBuilder.toString();

            // End of post portion
            String urlPath = req.getPathInfo();
            // check we have a URL!
            if (urlPath == null || urlPath.isEmpty()) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                res.getWriter().write("missing parameters");
                return;
            }

            String[] urlParts = urlPath.split("/");

            if (!isUrlValid(urlParts)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
                res.setStatus(HttpServletResponse.SC_OK);
                res.getWriter().write(jsonBodyString);
            }
        }


        private boolean isUrlValid(String[] urlPath) {
        if (urlPath[1].equals("left") || urlPath[1].equals("right")) {
            return true;
        }
        return false;
    }

    private boolean isJsonBodyValid(String jsonBodyString, HttpServletResponse res)
        throws IOException {
        try {
            JsonObject jsonBody = new JsonParser().parse(jsonBodyString).getAsJsonObject();
            // check if swiper is a number between 1 and 5000
            int swiper = jsonBody.get("swiper").getAsInt();
            if (swiper < 1 || swiper > 5000) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("Invalid swiper value. Must be a number between 1 and 5000.");
                return false;
            }

            // check if swipee is a number between 1 and 1000000
            int swipee = jsonBody.get("swipee").getAsInt();
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
}

