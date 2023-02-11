package com.example.assignment1SpringBoot.controller;


import com.example.assignment1SpringBoot.pojo.SwipeDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwipeServerlet{

  @RequestMapping(method=RequestMethod.POST,value="swipe/{para}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getSpecificQuestions(@PathVariable String para, @RequestBody SwipeDetails req) {


    // check if the url is valid
    if (!para.equals("left") && !para.equals("right")) {
      return new ResponseEntity<>("Invalid url parameter: should be left or right", HttpStatus.NOT_FOUND);
    }

    try {

      // check if request json body is valid
      if (!validSwiper(req.getSwiper())) {
        return new ResponseEntity<>("User not found: invalid swiper id", HttpStatus.NOT_FOUND);
      } else if (!validSwipee(req.getSwipee())) {
        return new ResponseEntity<>("User not found: invalid swipee id", HttpStatus.NOT_FOUND);
      } else if (!validComment(req.getComment())) {
        return new ResponseEntity<>("Invalid comments: comments can not exceed 256 characters", HttpStatus.NOT_FOUND);
      } else {
        return new ResponseEntity<>("Write successful", HttpStatus.OK);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return new ResponseEntity<>("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private boolean validSwiper(String swiper) {
    try {
      int swiperId = Integer.parseInt(swiper);
      if (swiperId < 1 || swiperId > 5000 || !isValidNumber(swiper)) {
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean validSwipee(String swipee) {
    try {
      int swipeeId = Integer.parseInt(swipee);
      if (swipeeId < 1 || swipeeId > 1000000 || !isValidNumber(swipee)) {
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  private boolean validComment(String comment) {
    if (comment.length() > 256) {
      return false;
    }
    return true;
  }

  private boolean isValidNumber(String s) {
    if (s == null || s.isEmpty()) return false;
    try {
      int digits = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }
}
