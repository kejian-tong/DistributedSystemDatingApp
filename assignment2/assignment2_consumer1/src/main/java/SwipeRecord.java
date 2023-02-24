import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SwipeRecord {
//  public static final String LEFT = "left";
//  public static final String RIGHT = "right";
//  public static final String LIKE = "Yes, I like you";
//  public static final String DISLIKE = "you are not my type, loser";
//  public static final Random random = new Random();
//  public static final String[] swipe = {LEFT, RIGHT};
//  public static final String[] comments = {LIKE, DISLIKE};

  private Integer swiper;
  private Integer swipee;
  private boolean isLike;
  private static Map<Integer, int[]> likeMap = new ConcurrentHashMap<>();
  private static Map<Integer, List<Integer>> listSwipeRight = new ConcurrentHashMap<>();

  public SwipeRecord() {
  }

  public static void addToNumOfLikeMap(Integer swiper, boolean isLike, Map<Integer, int[]> likeMap) {
    int[] likeOrDislike = likeMap.get(swiper);
    if (likeOrDislike == null) {
      likeOrDislike = new int[]{0, 0}; // issues to solve putIfAbsent
    }
    if (isLike) {
      likeOrDislike[0]++;
    } else {
      likeOrDislike[1]++;
    }
    likeMap.put(swiper, likeOrDislike);
  }

  /**
   *
   If !likeMap.keys().contains(swiper) {}
   if !likeMap.keys().contains(swiper) {
   likeMap.put(swiper, new int[] {0,0}) // listSwipeRight.put(swiper, swipeRightList);
   } else {
   likeMap.get(swiper) ++;
   }

   */

  public static void addToNumOfLikeMap(Integer swiper, Integer swipee, Map<Integer, List<Integer>> listSwipeRight) {
    List<Integer> swipeRightList = listSwipeRight.get(swiper);
    if (swipeRightList == null) {   // issue to need to be solved like first one
      swipeRightList = new CopyOnWriteArrayList<>();
    }
    if (swipeRightList.size() < 100 && !swipeRightList.contains(swipee)) {
      //
      swipeRightList.add(swipee); //
    }
    listSwipeRight.put(swiper, swipeRightList);
  }

}
