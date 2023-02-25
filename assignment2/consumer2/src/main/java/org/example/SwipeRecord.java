package org.example;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SwipeRecord {

  private Integer swiper;
  private Integer swipee;
  private boolean isLike;
  private static Map<Integer, Set<Integer>> listSwipeRight = new ConcurrentHashMap<>();

  public SwipeRecord() {
  }

//  public static void addToLikeMap(Integer swiper, Integer swipee) {
//    List<Integer> swipeRightList = listSwipeRight.get(swiper);
//    if (swipeRightList == null) {
//      swipeRightList = new CopyOnWriteArrayList<>();
//    }
//    if (swipeRightList.size() < 100 && !swipeRightList.contains(swipee)) {
//      //
//      swipeRightList.add(swipee); //
//    }
//    listSwipeRight.put(swiper, swipeRightList);
//  }

  public static void addToLikeMap(Integer swiper, Integer swipee, boolean isLike) {
    if (isLike) {
      Set<Integer> swipeRightSet = listSwipeRight.computeIfAbsent(swiper, k -> new HashSet<>());
      if (swipeRightSet.size() < 100) {
        swipeRightSet.add(swipee);
      }
    }
  }

  public static String toPrintString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Integer, Set<Integer>> entry : listSwipeRight.entrySet()) {
      Integer swiper = entry.getKey();
      Set<Integer> swipeRightSet = entry.getValue();
      sb.append(String.format("%d -> [%s]%n", swiper, String.join(",", swipeRightSet.toString())));
    }
    return sb.toString();
  }

}
