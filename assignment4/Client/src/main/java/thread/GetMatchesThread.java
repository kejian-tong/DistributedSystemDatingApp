package thread;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.api.MatchesApi;
import io.swagger.client.api.StatsApi;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import part2latency.Record;

public class GetMatchesThread extends GetThread {

  public GetMatchesThread(
      AtomicInteger numSuccessfulReqs,
      AtomicInteger numFailedReqs,
      BlockingQueue<List<Record>> recordsBuffer, CountDownLatch getLatch) {
    super(numSuccessfulReqs, numFailedReqs, recordsBuffer, getLatch);
  }


  @Override
  public void run() {
    MatchesApi matchesApi = new MatchesApi(new ApiClient());
    matchesApi.getApiClient().setBasePath(LoadTestConfig.GET_MATCHES_URL);


    int apiType = 1;
    int i = 0;
    List<Record> records = new ArrayList<>();

    while (i < 1250){
      Record record = this.sendSingleRequest(matchesApi, null, apiType);
      records.add(record);
    }

    this.getLatch.countDown();
    try {
      recordsBuffer.put(records);
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to put local list of records from " + Thread.currentThread().getName() + " to the GET MATCHES buffer queue. " + e);
    }
  }



}
