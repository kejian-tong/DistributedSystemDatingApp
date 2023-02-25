package org.example;


public class SwipeDetails {
  private Integer swiper;
  private Integer swipee;
  private String comment;
  private boolean isLike;


  public Integer getSwiper() {
    return swiper;
  }

  public Integer getSwipee() {
    return swipee;
  }

  public String getComment() {
    return comment;
  }

  public void setSwiper(Integer swiper) {
    this.swiper = swiper;
  }

  public boolean getLike() {
    return isLike;
  }

  public void setLike(boolean like) {
    isLike = like;
  }
}


