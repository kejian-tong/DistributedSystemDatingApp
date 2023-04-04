package CalculateLatency;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class MultiThreadClient {
  private static final int numOfThreads = 150;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger();
  private static AtomicInteger numOfFailReq = new AtomicInteger();
  private static final int totalRequest = 500000;
  private static BlockingQueue<LatencyRecord> records = new ArrayBlockingQueue<>(totalRequest);

  public static void main(String[] args) throws InterruptedException, IOException {

    long start = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    LatencyStats latencyStats = new LatencyStats();
    AtomicBoolean running = new AtomicBoolean(true);
    Thread getThread = new Thread(new GetThread(latencyStats, running));


    for (int i = 0; i < numOfThreads; i++) {
      int numOfRequest = totalRequest / numOfThreads;
      if (i == numOfThreads - 1) {
        numOfRequest += totalRequest % numOfThreads;
      }
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfRequest, completed, records);
      Thread thread = new Thread(singleThreadClient);
      singleThreadClients[i] = singleThreadClient;
      thread.start();
    }

//    System.out.println("Total post threads start");
    // Start the GetThread after all posting threads have started
    getThread.start(); // comment for test doPost

    completed.await(); // wait for all threads to complete
//    System.out.println("Total post threads end");

    latencyStats.printStats(); // Print the Get latency min max and mean

    // Stop the GetThread after all posting threads have finished
    running.set(false);
    getThread.join(); // wait for the GetThread to complete

    for (int i = 0; i < numOfThreads; i++) {
      numOfSuccessReq.addAndGet(singleThreadClients[i].getNumOfSuccessReq());
      numOfFailReq.addAndGet(singleThreadClients[i].getGetNumOfFailReq());
    }

    long end = System.currentTimeMillis();
    long walltime = (end - start) / 1000;
    int numOfRequests = numOfSuccessReq.get() + numOfFailReq.get();
    long throughput = numOfRequests / walltime;

    System.out.println("\n====== POST requests results ======\n");
    System.out.println("Total post throughput: " + throughput + " req/s");
    System.out.println("Number of successful requests: " + numOfSuccessReq);
    System.out.println("Number of fail requests: " + numOfFailReq);

    LatencyRecord[] latencyRecords = records.toArray(new LatencyRecord[records.size()]);
    Arrays.sort(latencyRecords, Comparator.comparingLong(LatencyRecord::getLatency));


    // Continue with min, max, and mean calculations...
    long sum = 0;
    for (LatencyRecord record : latencyRecords) {
      sum += record.getLatency();
    }
    long mean = Math.round((double) sum / latencyRecords.length);
    System.out.println("Mean latency time: " + mean + " ms");

    long max = latencyRecords[latencyRecords.length - 1].getLatency();
    System.out.println("Max latency time: " + max + " ms");

    long min = latencyRecords[0].getLatency();
    System.out.println("Min latency time: " + min + " ms");

  }
}
