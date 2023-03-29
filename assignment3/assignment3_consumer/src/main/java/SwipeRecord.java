import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SwipeRecord {

  private Integer swiper;
  private Integer swipee;
  private boolean isLike;
  private static Map<Integer, int[]> likeOrDislikeMap = new ConcurrentHashMap<>();
  public static Map<Integer, Set<Integer>> listSwipeRight = new ConcurrentHashMap<>();


  public SwipeRecord() {
  }

//  public static Map<Integer, int[]> getLikeOrDislikeMap() {
//    return likeOrDislikeMap;
//  }
//
//  public static void addToLikeOrDislikeMap(Integer swiper, boolean isLike) {
//    int[] likeOrDislike = likeOrDislikeMap.get(swiper);
//    if (likeOrDislike == null) {
//      likeOrDislike = new int[]{0, 0};
//    }
//    if (isLike) {
//      likeOrDislike[0]++;
//    } else {
//      likeOrDislike[1]++;
//    }
//    likeOrDislikeMap.put(swiper, likeOrDislike);
//  }


  public static void addToLikeMap(Integer swiper, Integer swipee, boolean isLike) {
    if (isLike) {
      Set<Integer> swipeRightSet = listSwipeRight.computeIfAbsent(swiper, k -> new HashSet<>());
        swipeRightSet.add(swipee);
    }
  }
}




