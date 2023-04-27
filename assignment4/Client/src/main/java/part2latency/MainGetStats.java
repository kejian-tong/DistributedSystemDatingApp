package part2latency;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import thread.GetStatsThread;

public class MainGetStats {
  private static final RunningMetrics getMetrics = new RunningMetrics();
  private static Long getStatsStartPostTime, getStatsEndPostTime;

  public static void main(String[] args) throws InterruptedException {


    final AtomicInteger numSuccessfulGetStatsReqs = new AtomicInteger(0);
    final AtomicInteger numFailedGetStatsReqs = new AtomicInteger(0);

    BlockingQueue<List<Record>> getStatsRecordBuffer = new LinkedBlockingQueue<>(200);



    getStatsStartPostTime = System.currentTimeMillis();
    CountDownLatch getStatsLatch = new CountDownLatch(200);
    //Start the GET Stats thread
    for (int i = 0; i < 200; i++) {
      new Thread(
          new GetStatsThread(numSuccessfulGetStatsReqs, numFailedGetStatsReqs, getStatsRecordBuffer, getStatsLatch)
      );
    }

    getStatsLatch.await();
    getStatsEndPostTime = System.currentTimeMillis();

    // Update Metrics for GET records
    for (int i = 0; i < 200; i++) {
      List<Record> threadRecords = getStatsRecordBuffer.take();
      getMetrics.updateRunningMetrics(threadRecords);
    }

    float wallTime = (getStatsEndPostTime - getStatsStartPostTime)/1000f;
    System.out.println("\n\n====== GET Stats requests results ======");
    System.out.println("Successful Requests:" + numSuccessfulGetStatsReqs);
    System.out.println("Unsuccessful Requests:" + numFailedGetStatsReqs);
    System.out.println("Mean Response Time (ms): " + (float)getMetrics.getSumLatency() / getMetrics.getNumTotalRecord());
    System.out.println("Min Response Time (ms): " + getMetrics.getMinLatency());
    System.out.println("Max Response Time (ms): " + getMetrics.getMaxLatency());
    // For A4 test:
    System.out.println("Throughput: " + numSuccessfulGetStatsReqs.get() / wallTime + " req/s");

  }
}
