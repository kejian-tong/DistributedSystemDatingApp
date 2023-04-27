package lessblocking;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.PostRequest;
import request.PostRequestGenerator;
import thread.AbsSendRequestThread;

public class SendRequestLessBlockThread extends AbsSendRequestThread implements Runnable{
  // numTakenReqs: Track the accumulated number of requests taken by all threads.
  private final AtomicInteger numTakenReqs;

  // NUM_REQUEST_BATCH: The number of requests taken by a thread in each round.
  private final static int NUM_REQUEST_BATCH = 500;


  public SendRequestLessBlockThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs,
      AtomicInteger numTakenReqs) {
    super(numSuccessfulReqs, numFailedReqs, latch);
    this.numTakenReqs = numTakenReqs;
  }

  /**
   * Note: The blocking part (i.e. increment global counter: numTakenReqs) in this model only happens once every NUM_REQUEST_BATCH requests.
   */
  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.SWIPE_URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);

    int localNumOfSuccessfulReqs = 0;
    int localNumOfFailedReqs = 0;

    while (this.numTakenReqs.get() < LoadTestConfig.NUM_TOTAL_REQUESTS) {
      int taken = Math.min(LoadTestConfig.NUM_TOTAL_REQUESTS - this.numTakenReqs.get(), NUM_REQUEST_BATCH);
      this.numTakenReqs.getAndAdd(taken);
      System.out.println("!!! " + "Thread:" + Thread.currentThread().getName() + " NEWLY TAKEN "+  taken + ". Now numTakenReqs: " + this.numTakenReqs.get());
      int curRoundNumOfSuccessfulReqs = 0;
      while (curRoundNumOfSuccessfulReqs < taken) {
        boolean isSuccess = this.sendSingleRequest(
            PostRequestGenerator.generateSingleRequest(), swipeApi, this.numSuccessfulReqs, this.numFailedReqs);
        if (isSuccess) {
          curRoundNumOfSuccessfulReqs++;
          localNumOfSuccessfulReqs++;
          // System.out.println("Thread:" + Thread.currentThread().getName() + " Successfully sent a request. " + " Local Success cnt:" + localNumOfSuccessfulReqs + " Taken cnt:" + this.numTakenReqs.get());
        } else {
          localNumOfFailedReqs++;
          //System.out.println("Thread:" + Thread.currentThread().getName() + " Failed to sent a request.");
        }
      }
      //System.out.println(" ==== Thread:" + Thread.currentThread().getName() + " Finished batch reqs. Keep taking. Local Success cnt:" + localNumOfSuccessfulReqs + " Taken cnt:" + this.numTakenReqs.get() + " =====");
    }

    this.numSuccessfulReqs.getAndAdd(localNumOfSuccessfulReqs);
    this.numFailedReqs.getAndAdd(localNumOfFailedReqs);

    this.latch.countDown();
  }

  /**
   * Send a single POST request to server. If succeeded within MAX_RETRY, return true;
   * Otherwise, return false;
   * NOTE: Compared to the sendSingleRequest method in AbsSendRequestThread class, which is used in the Average Model and Producer-Consumer Model,
   * this implementation don't have any "blockings", since it doesn't MODIFY any global objects.(e.g. counters:  numSuccessfulReqs, numFailedReqs)
   */

  public boolean sendSingleRequest(PostRequest request, SwipeApi swipeApi, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
    int retry = LoadTestConfig.MAX_RETRY;

    while (retry > 0) {
      try {
        ApiResponse res = swipeApi.swipeWithHttpInfo(request.getBody(), request.getSwipeDir());
        // System.out.println("Thread:" + Thread.currentThread().getName() + " Success cnt:" + numSuccessfulReqs.get() + "Status:" + res.getStatusCode());
        return true;
      } catch (ApiException e) {
        System.out.println("Failed to send request: " + e.getCode() + ": " + e.getResponseBody() + ".request.Request details:"
            + request.getSwipeDir() + " " + request.getBody().toString() + ". Go retry");
        retry --;
      }
    }

    return false;
  }
}
