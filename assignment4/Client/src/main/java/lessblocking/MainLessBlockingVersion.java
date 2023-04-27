package lessblocking;

import config.LoadTestConfig;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MainLessBlockingVersion {

  public static void main(String[] args) throws InterruptedException {
    // count successful/unsuccessful requests
    CountDownLatch latch = new CountDownLatch(LoadTestConfig.NUM_THREADS);
    final AtomicInteger numSuccessfulReqs = new AtomicInteger(0);
    final AtomicInteger numFailedReqs = new AtomicInteger(0);
    final AtomicInteger numTakenReqs = new AtomicInteger(0);
    long startTime = System.currentTimeMillis();

    for (int i = 0; i < LoadTestConfig.NUM_THREADS; i++) {
      Runnable thread = new SendRequestLessBlockThread(latch, numSuccessfulReqs, numFailedReqs,
          numTakenReqs);
      new Thread(thread).start();
    }

    latch.await();
    long endTime = System.currentTimeMillis();
    float wallTime = (endTime - startTime)/1000f;
    System.out.println("Successful Requests:" + numSuccessfulReqs);
    System.out.println("Unsuccessful Requests:" + numFailedReqs);
    System.out.println("Number of Threads: " + LoadTestConfig.NUM_THREADS);
    System.out.println("Multi-thread wall time:" + wallTime + "s");
    System.out.println("Throughput: " + numSuccessfulReqs.get() / wallTime + " req/s");

  }
}
