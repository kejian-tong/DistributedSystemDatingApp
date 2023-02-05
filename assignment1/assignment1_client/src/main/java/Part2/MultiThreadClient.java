package Part2;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class MultiThreadClient {
  private static final int numOfThreads = 100;
  private static final int numOfReq = 5000;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger();
  private static AtomicInteger numOfFailReq = new AtomicInteger();

  private static BlockingQueue<LatencyRecord> records = new ArrayBlockingQueue<>(numOfThreads * numOfReq);


  public static void main(String[] args) throws InterruptedException, IOException {

    long start = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    for(int i = 0; i < numOfThreads; i++) {
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfReq, completed, records);
      Thread thread = new Thread(singleThreadClient);
      singleThreadClients[i] = singleThreadClient;
      thread.start();
    }

    completed.await(); // wait all threads completed

    for(int i = 0; i < numOfThreads; i++) {
      numOfSuccessReq.addAndGet(singleThreadClients[i].getNumOfSuccessReq());
      numOfFailReq.addAndGet(singleThreadClients[i].getGetNumOfFailReq());
    }

    long end = System.currentTimeMillis();
    long walltime = (end - start) / 1000;
    int numOfRequests = numOfSuccessReq.get() + numOfFailReq.get();
    long throughput = numOfRequests / walltime;
    System.out.println("Total throughput: " + throughput + " req/s");

    LatencyRecord[] latencyRecords = records.toArray(new LatencyRecord[records.size()]);

    long sum = 0;
    for(LatencyRecord record : latencyRecords) {
      sum += record.getLatency();
    }
    long mean = sum / latencyRecords.length;
    System.out.println("Mean response time: " + mean + " ms");

    Arrays.sort(latencyRecords, (o1, o2) -> (int)(o1.getLatency() - o2.getLatency()));
    int medianIndex = latencyRecords.length / 2;
    long median = latencyRecords[medianIndex].getLatency();
    System.out.println("Median response time: " + median + " ms");

    int p99Index = (int) Math.ceil(latencyRecords.length * 0.99) - 1;
    long p99 = latencyRecords[p99Index].getLatency();
    System.out.println("99th percentile response time: " + p99 + " ms");

    long max = latencyRecords[latencyRecords.length - 1].getLatency();
    System.out.println("Max response time: " + max + " ms");

    long min = latencyRecords[0].getLatency();
    System.out.println("Min response time: " + min + " ms");

    // get the data for plot Performance for task 4
    Map<Long, Integer> requestsPerSecond = new HashMap<>();
    for(LatencyRecord latencyRecord: latencyRecords) {
        long startTime = latencyRecord.getStartTime() / 1000; // convert to seconds
        int count = requestsPerSecond.getOrDefault(startTime, 0);
        requestsPerSecond.put(startTime, count + 1);
    }

    FileWriters.run(records);
    FileWriters.runStats(requestsPerSecond); //task 4 data for plot

  }
}
