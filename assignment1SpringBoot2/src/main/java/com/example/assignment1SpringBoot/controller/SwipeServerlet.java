package com.example.assignment1SpringBoot.controller;

import com.example.assignment1SpringBoot.pojo.ResponseMsg;
import com.example.assignment1SpringBoot.pojo.SwipeDetails;
import com.google.gson.Gson;
import javax.servlet.http.HttpServlet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping (path = "assignment1_springserver/swipe")
public class SwipeServerlet extends HttpServlet {


  @RequestMapping(method=RequestMethod.POST,value="/{para}/*")
  public ResponseEntity getSpecificQuestions(@PathVariable String para, @RequestBody String req) {
    Gson gson = new Gson();
    ResponseMsg responseMsg = new ResponseMsg();
    responseMsg.setMessage("");

    // check if the url is valid
    if (!para.equals("left") && !para.equals("right")) {
      responseMsg.setMessage("Invalid url parameter: should be left or right");
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMsg));
    }

    try {
      SwipeDetails swipeDetails = gson.fromJson(req, SwipeDetails.class);

      // check if request json body is valid
      if (!validSwiper(swipeDetails.getSwiper())) {
        responseMsg.setMessage("User not found: invalid swiper id");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMsg));
      } else if (!validSwipee(swipeDetails.getSwipee())) {
        responseMsg.setMessage("User not found: invalid swipee id");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(gson.toJson(responseMsg));
      } else if (!validComment(swipeDetails.getComment())) {
        responseMsg.setMessage("Invalid comments: comments can not exceed 256 characters");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(gson.toJson(responseMsg));
      } else {
        responseMsg.setMessage("Write successful");
        return ResponseEntity.status(HttpStatus.OK).body(gson.toJson(responseMsg));
      }
    } catch (Exception e) {
      e.printStackTrace();
      responseMsg.setMessage(e.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(gson.toJson(responseMsg));
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
