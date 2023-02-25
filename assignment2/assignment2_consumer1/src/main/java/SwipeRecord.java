import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SwipeRecord {

  private Integer swiper;
  private Integer swipee;
  private boolean isLike;
  private static Map<Integer, int[]> likeOrDislikeMap = new ConcurrentHashMap<>();
//  private static Map<Integer, List<Integer>> listSwipeRight = new ConcurrentHashMap<>();


  public static Map<Integer, int[]> getLikeOrDislikeMap() {
    return likeOrDislikeMap;
  }

  public static void addToLikeOrDislikeMap(Integer swiper, boolean isLike) {
    int[] likeOrDislike = likeOrDislikeMap.get(swiper);
    if (likeOrDislike == null) {
      likeOrDislike = new int[]{0, 0};
    }
    if (isLike) {
      likeOrDislike[0]++;
    } else {
      likeOrDislike[1]++;
    }
    likeOrDislikeMap.put(swiper, likeOrDislike);
  }


  public static String toNewString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, int[]> entry : likeOrDislikeMap.entrySet()) {
      Integer swiper = entry.getKey();
      int[] likeOrDislike = entry.getValue();
      sb.append("Swiper ID: ").append(swiper).append(", Likes: ").append(likeOrDislike[0])
          .append(", Dislikes: ").append(likeOrDislike[1]).append("\n");
    }
    return sb.toString();
  }
}




