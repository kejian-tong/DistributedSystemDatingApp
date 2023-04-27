package assignment4.config.datamodel;

public class SwipeDetails {
  private final static int MAX_COMMENT_LEN = 256;
  public static final int MAX_SWIPER_ID = 5000;
  public static final int MAX_SWIPEE_ID = 5000;

  public final static String LEFT = "left";
  public final static String RIGHT = "right";
  private String swiper;
  private String swipee;
  private String comment;
  private String direction;

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getSwiper() {
    return swiper;
  }

  public void setSwiper(String swiper) {
    this.swiper = swiper;
  }

  public String getSwipee() {
    return swipee;
  }

  public void setSwipee(String swipee) {
    this.swipee = swipee;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }


  public boolean isSwiperValid() {
    int id_int = Integer.parseInt(this.swiper);
    return 1 <= id_int && id_int <= MAX_SWIPER_ID ;
  }

  public boolean isSwipeeValid() {
    int id_int = Integer.parseInt(this.swipee);
    return 1 <= id_int && id_int <= MAX_SWIPEE_ID ;
  }
  public boolean isCommentValid() {
    return this.comment.length() <= MAX_COMMENT_LEN;
  }

}
