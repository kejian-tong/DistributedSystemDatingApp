package thread;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SwipeApi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import part2latency.Record;
import part2latency.RequestType;
import request.PostRequest;
import request.PostRequestGenerator;

/**
 *  * A Thread that sends Post requests with Less-Blocking/Batch Model
 */

public class PostThread extends AbsSendRequestThread implements Runnable{
  // numTakenReqs: Track the accumulated number of requests taken by all threads.
  private final AtomicInteger numTakenReqs;
  private final BlockingQueue<List<Record>> recordsBuffer;

  // NUM_REQUEST_BATCH: The number of requests taken by a thread in each round.
  private final static int NUM_REQUEST_BATCH = 500;


  public PostThread(CountDownLatch latch, AtomicInteger numSuccessfulReqs, AtomicInteger numFailedReqs,
      AtomicInteger numTakenReqs, BlockingQueue<List<Record>> recordsBuffer) {
    super(numSuccessfulReqs, numFailedReqs, latch);
    this.numTakenReqs = numTakenReqs;
    this.recordsBuffer = recordsBuffer;
  }

  /**
   * Note: The blocking part (i.e. increment global counter: numTakenReqs) in this model only happens once every NUM_REQUEST_BATCH requests.
   */
  @Override
  public void run() {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(LoadTestConfig.SWIPE_URL);

    SwipeApi swipeApi = new SwipeApi(apiClient);
    List<Record> records = new ArrayList<>();

    int localNumOfSuccessfulReqs = 0;
    int localNumOfFailedReqs = 0;

    while (this.numTakenReqs.get() < LoadTestConfig.NUM_TOTAL_REQUESTS) {
      int taken = Math.min(LoadTestConfig.NUM_TOTAL_REQUESTS - this.numTakenReqs.get(), NUM_REQUEST_BATCH);
      this.numTakenReqs.getAndAdd(taken);
      System.out.println("!! POST BATCH !! " + "Thread:" + Thread.currentThread().getName() + " NEWLY TAKEN "+  taken + ". Now numTakenReqs: " + this.numTakenReqs.get());
      int curRoundNumOfSuccessfulReqs = 0;
      while (curRoundNumOfSuccessfulReqs < taken) {
        Record record = this.sendSingleRequest(PostRequestGenerator.generateSingleRequest(), swipeApi);

        if (record.getResponseCode() == LoadTestConfig.POST_SUCCESS_CODE) {
          curRoundNumOfSuccessfulReqs++;
          localNumOfSuccessfulReqs++;
          // System.out.println("Thread:" + Thread.currentThread().getName() + " Successfully sent a request. " + " Local Success cnt:" + localNumOfSuccessfulReqs + " Taken cnt:" + this.numTakenReqs.get());
        } else {
          localNumOfFailedReqs++;
          //System.out.println("Thread:" + Thread.currentThread().getName() + " Failed to sent a request.");
        }
        records.add(record);
      }
      //System.out.println(" ==== Thread:" + Thread.currentThread().getName() + " Finished batch reqs. Keep taking. Local Success cnt:" + localNumOfSuccessfulReqs + " Taken cnt:" + this.numTakenReqs.get() + " =====");
    }

    this.numSuccessfulReqs.getAndAdd(localNumOfSuccessfulReqs);
    this.numFailedReqs.getAndAdd(localNumOfFailedReqs);

    this.latch.countDown();

    try {
      recordsBuffer.put(records);
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to put local list of records from " + Thread.currentThread().getName() + " to the buffer queue. " + e);
    }
  }

  /**
   * Send a single POST request to server. If succeeded within MAX_RETRY, return true;
   * Otherwise, return false;
   * NOTE: Compared to the sendSingleRequest method in AbsSendRequestThread class, which is used in the Average Model and Producer-Consumer Model,
   * this implementation don't have any "blocking"s, since it doesn't MODIFY any global objects.(e.g. counters:  numSuccessfulReqs, numFailedReqs)
   */
  private Record sendSingleRequest(PostRequest request, SwipeApi swipeApi) {
    int retry = LoadTestConfig.MAX_RETRY;

    long startTime = System.currentTimeMillis();
    long endTime;

    while (retry > 0) {
      try {
        ApiResponse<Void> res = swipeApi.swipeWithHttpInfo(request.getBody(), request.getSwipeDir());

        endTime = System.currentTimeMillis();
        // numSuccessfulReqs.getAndIncrement();
        // System.out.println("POST: Thread:" + Thread.currentThread().getName() + " Success cnt:" + numSuccessfulReqs.get() + " Status:" + res.getStatusCode() + "Msg:"+ res.getData());
        return new Record(startTime, RequestType.POST, (int)(endTime-startTime), res.getStatusCode(), Thread.currentThread().getName());
      } catch (ApiException e) {
//        System.out.println("Failed to send request: " + e.getCode() + ": " + e.getResponseBody() + ".request.Request details:"
//            + request.getSwipeDir() + " " + request.getBody().toString() + ". Go retry");
        System.out.println("POST: RETRY Request: " + e.getMessage() + " " +e.getResponseBody() + " " +e.getCode());
        retry --;
        if (retry == 0) {
          endTime = System.currentTimeMillis();
          // numFailedReqs.getAndIncrement();
          return new Record(startTime, RequestType.POST, (int)(endTime-startTime), e.getCode(), Thread.currentThread().getName());
        }
      }
    }

    return null;
  }
}
