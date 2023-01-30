package Part1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SwipeApi;
import io.swagger.client.model.SwipeDetails;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class SingleThreadClient {
  public int numOfSuccessReq = 0;
  public int numOfFailReq = 0;
  private int numOfReq;

  private  String basePath = "http://localhost:8080/assignment1_server_war_exploded";
  private CountDownLatch completed;

  public SingleThreadClient(int numOfReq, CountDownLatch completed) {
    this.numOfReq = numOfReq;
    this.completed = completed;
  }


  public void run() {
    ApiClient apiClient = new ApiClient();
    SwipeApi swipeApi = new SwipeApi(apiClient);
    apiClient.setBasePath(basePath);

    for(int i = 0; i < numOfReq; i++) {
      boolean success = false;
      for(int j = 0; j <= Module.MAX_RE_TRY; j++) {
        SwipeDetails body =  new SwipeDetails();
        // randomly get the data
        String leftorright = Module.swipe[new Random().nextInt(Module.swipe.length)];
        body.setSwiper(String.valueOf(Module.random.nextInt(5000)));
        body.setSwipee(String.valueOf(Module.random.nextInt(1000000)));
        body.setComment(Module.comments[new Random().nextInt(Module.comments.length)]);

        try {
          swipeApi.swipe(body, leftorright);
          numOfSuccessReq++;
          success = true;
          break;
        } catch (ApiException e) {
          System.err.println("Call SwipeApi error");
          System.out.println(e.getCode());
          e.printStackTrace();
        }
      }
      if(!success) {
        numOfFailReq++;
      }
    }
    completed.countDown();
    System.out.println(Thread.currentThread().getName() + "completed");
  }
  
  public int getNumOfSuccessReq(){
    return numOfSuccessReq;
  }
  
  public int getGetNumOfFailReq(){
    return numOfFailReq;
  }

  public int getNumOfReq(){
    return numOfReq;
  }

}
