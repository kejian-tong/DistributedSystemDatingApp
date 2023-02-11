package com.example.assignment1SpringBoot.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SwipeDetails {
  @JsonProperty("swiper")
  private String swiper;

  @JsonProperty("swipee")
  private String swipee;

  @JsonProperty("comment")
  private String comment;


  public String getSwiper() {
    return swiper;
  }

  public String getSwipee() {
    return swipee;
  }

  public String getComment() {
    return comment;
  }

}
