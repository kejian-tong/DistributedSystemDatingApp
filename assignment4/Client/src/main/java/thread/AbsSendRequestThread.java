package thread;

import config.LoadTestConfig;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import request.PostRequest;

public abstract class AbsSendRequestThread implements Runnable{
  protected final AtomicInteger numSuccessfulReqs;

  protected final AtomicInteger numFailedReqs;
  protected final CountDownLatch latch;

  public AbsSendRequestThread(AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs,
      CountDownLatch latch) {
    this.numSuccessfulReqs = numSuccessfulReqs;
    this.numFailedReqs = numFailedReqs;
    this.latch = latch;
  }

  /**
   * Send a single POST request to server. If succeeded within MAX_RETRY, increment the global counter:numSuccessfulReqs and return true;
   * Otherwise, increment the global counter: numFailedReqs and return false;
   */
//  @Override
//  public Record sendSingleRequest(PostRequest request, SwipeApi swipeApi, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs) {
//    int retry = LoadTestConfig.MAX_RETRY;
//
//    while (retry > 0) {
//      try {
//        ApiResponse res = swipeApi.swipeWithHttpInfo(request.getBody(), request.getSwipeDir());
//        numSuccessfulReqs.getAndIncrement();
//        System.out.println("Thread:" + Thread.currentThread().getName() + " Success cnt:" + numSuccessfulReqs.get() + " Status:" + res.getStatusCode());
//        return true;
//      } catch (ApiException e) {
//        System.out.println("Failed to send request: " + e.getCode() + ": " + e.getResponseBody() + ".request.Request details:"
//            + request.getSwipeDir() + " " + request.getBody().toString() + ". Go retry");
//        retry --;
//      }
//    }
//
//    numFailedReqs.getAndIncrement();
//    return false;
//  }
}
