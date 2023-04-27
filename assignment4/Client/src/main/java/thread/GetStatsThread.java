package thread;

import config.LoadTestConfig;
import io.swagger.client.ApiClient;
import io.swagger.client.api.MatchesApi;
import io.swagger.client.api.StatsApi;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import part2latency.Record;

public class GetStatsThread extends GetThread {

  public GetStatsThread(
      AtomicInteger numSuccessfulReqs,
      AtomicInteger numFailedReqs,
      BlockingQueue<List<Record>> recordsBuffer, CountDownLatch getLatch) {
    super(numSuccessfulReqs, numFailedReqs, recordsBuffer, getLatch);
  }


  @Override
  public void run() {
    StatsApi statsApi = new StatsApi(new ApiClient());
    statsApi.getApiClient().setBasePath(LoadTestConfig.GET_STATS_URL);


    int apiType = -1;
    int i = 0;
    List<Record> records = new ArrayList<>();

    while (i < 1250){
      Record record = this.sendSingleRequest(null, statsApi, apiType);
      records.add(record);
    }

    this.getLatch.countDown();
    try {
      recordsBuffer.put(records);
    } catch (InterruptedException e) {
      throw new RuntimeException("Failed to put local list of records from " + Thread.currentThread().getName() + " to the GET STATS buffer queue. " + e);
    }
  }
}
