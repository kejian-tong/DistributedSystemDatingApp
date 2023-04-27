package part2latency;

import config.LoadTestConfig;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import thread.GetMatchesThread;
import thread.GetStatsThread;
import thread.PostThread;

public class MainGetMatches {
  private static final RunningMetrics getMetrics = new RunningMetrics();
  private static Long getMatchesStartPostTime, getMatchesEndPostTime;
  public static void main(String[] args) throws InterruptedException {
    final AtomicInteger numSuccessfulGetMatchesReqs = new AtomicInteger(0);
    final AtomicInteger numFailedGetMatchesReqs = new AtomicInteger(0);

    BlockingQueue<List<Record>> getMatchesRecordBuffer = new LinkedBlockingQueue<>(128);
    getMatchesStartPostTime = System.currentTimeMillis();
    CountDownLatch getMatchesLatch = new CountDownLatch(128);
    //Start the GET Matches thread
    for (int i = 0; i < 128; i++) {
      new Thread(
          new GetStatsThread(numSuccessfulGetMatchesReqs, numFailedGetMatchesReqs, getMatchesRecordBuffer, getMatchesLatch)
      );
    }

    getMatchesLatch.await();
    getMatchesEndPostTime = System.currentTimeMillis();

    // Update Metrics for GET records
    for (int i = 0; i < 128; i++) {
      List<Record> threadRecords = getMatchesRecordBuffer.take();
      getMetrics.updateRunningMetrics(threadRecords);
    }


    float wallTime = (getMatchesEndPostTime - getMatchesStartPostTime)/1000f;
    System.out.println("\n\n====== GET Matches requests results ======");
    System.out.println("Successful Requests:" + numSuccessfulGetMatchesReqs);
    System.out.println("Unsuccessful Requests:" + numFailedGetMatchesReqs);
    System.out.println("Mean Response Time (ms): " + (float)getMetrics.getSumLatency() / getMetrics.getNumTotalRecord());
    System.out.println("Min Response Time (ms): " + getMetrics.getMinLatency());
    System.out.println("Max Response Time (ms): " + getMetrics.getMaxLatency());
    // For A4 test:
    System.out.println("Throughput: " + numSuccessfulGetMatchesReqs.get() / wallTime + " req/s");


  }


}
