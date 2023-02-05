package Part1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class SingleThreadClient implements Runnable {
  private int numOfSuccessReq = 0;
  private int numOfFailReq = 0;
  private int numOfReq;

//  private  String basePath = "http://localhost:8080/assignment1_server_war_exploded";
  private String basePath = "http://34.217.81.112:8080/assignment1_server_war";
  private CountDownLatch completed;

  public SingleThreadClient(int numOfReq, CountDownLatch completed) {
    this.numOfReq = numOfReq;
    this.completed = completed;
  }

@Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    SwipeApi swipeApi = new SwipeApi(apiClient);
    apiClient.setBasePath(basePath);

    for(int i = 0; i < numOfReq; i++) {
      boolean success = false;
      SwipeDetails body =  new SwipeDetails();
      // randomly get the data
      String leftorright = Module.swipe[new Random().nextInt(Module.swipe.length)];
      body.setSwiper(String.valueOf(ThreadLocalRandom.current().nextInt(1, 5001)));
      body.setSwipee(String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000001)));
      body.setComment(Module.comments[new Random().nextInt(Module.comments.length)]);

      for(int j = 0; j < Module.MAX_RE_TRY; j++) {
        try {
          swipeApi.swipeWithHttpInfo(body, leftorright);
          numOfSuccessReq++;
          success = true;
          break;
        } catch (ApiException e) {
          e.printStackTrace();
          System.err.println("Calling SwipeApi error");
          System.out.println(e.getCode());
        }
      }
      if(!success) {
        numOfFailReq++;
      }
    }
    completed.countDown();
  }
  
  public int getNumOfSuccessReq(){
    return numOfSuccessReq;
  }
  
  public int getGetNumOfFailReq(){
    return numOfFailReq;
  }

}
