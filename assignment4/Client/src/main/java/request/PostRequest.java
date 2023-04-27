package request;

import io.swagger.client.model.SwipeDetails;

/**
 * Represent a POST request with randomly generated swiping direction and body.
 * swipeDir - either “left” or “right”
 * swiperId - between 1 and 5000
 * swipeeId - between 1 and 1000000
 * comment - random string of 256 characters
 */
public class PostRequest {
  private final String swipeDir;
  private final SwipeDetails body;

  public PostRequest(String swipeDir, SwipeDetails body) {
    this.swipeDir = swipeDir;
    this.body = body;
  }

  public String getSwipeDir() {
    return swipeDir;
  }

  public SwipeDetails getBody() {
    return body;
  }
}
