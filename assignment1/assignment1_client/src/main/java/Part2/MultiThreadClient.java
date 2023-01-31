package Part2;

import Part1.SingleThreadClient;
import io.swagger.client.ApiClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {

  private static final int numOfThreads = 100;
  private static final int numOfReq = 5000;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger();
  private static AtomicInteger numOfFailReq = new AtomicInteger();
  private static List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

  public static void main(String[] args) throws InterruptedException {
    long start = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    for (int i = 0; i < numOfThreads; i++) {
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfReq, completed);
      Thread thread = new Thread(singleThreadClient);
      singleThreadClients[i] = singleThreadClient;
      thread.start();
    }

    completed.await(); // wait all threads completed

    for (int i = 0; i < numOfThreads; i++) {
      System.out.println(singleThreadClients[i]);
    }

    for (int i = 0; i < numOfThreads; i++) {
      System.out.println(singleThreadClients[i].getNumOfSuccessReq());
      numOfSuccessReq.addAndGet(singleThreadClients[i].getNumOfSuccessReq());
      numOfFailReq.addAndGet(singleThreadClients[i].getGetNumOfFailReq());
    }

    long end = System.currentTimeMillis();
    long walltime = (end - start) / 1000;
    long throughput = numOfReq / walltime;

    System.out.println("Number of successful requests: " + numOfSuccessReq);
    System.out.println("Number of fail requests: " + numOfFailReq);
    System.out.println("Total throughput: " + throughput + "req/s");

    long sum = 0;
    long minLatency = Long.MAX_VALUE;
    long maxLatency = Long.MIN_VALUE;
    for (long latency : latencies) {
      sum += latency;
      minLatency = Math.min(minLatency, latency);
      maxLatency = Math.max(maxLatency, latency);
    }
    long meanLatency = sum / latencies.size();
    long medianLatency = latencies.get(latencies.size() / 2);
    long p99Latency = latencies.get((int) (0.99 * latencies.size()));

    System.out.println("Mean Latency: " + meanLatency + "ms");
    System.out.println("Median Latency: " + medianLatency + "ms");
    System.out.println("p99Latency: " + p99Latency + "ms");

  }

}
//public class MultiThreadClient {
//  private static final int NUM_THREADS = 100;
//  private static final int NUM_REQUESTS = 5000;
//  private static List<Long> latencies = new ArrayList<>();
//
//  public static void main(String[] args) throws InterruptedException {
//    CountDownLatch completed = new CountDownLatch(NUM_THREADS);
//    long startTime = System.currentTimeMillis();
//
//    SingleThreadClient[] singleThreadClients = new SingleThreadClient[NUM_THREADS];
//
//    for (int i = 0; i < NUM_THREADS; i++) {
//      SingleThreadClient singleThreadClient = new SingleThreadClient(NUM_REQUESTS, completed, latencies);
//      Thread thread = new Thread(singleThreadClient);
//      singleThreadClients[i] = singleThreadClient;
//      thread.start();
//    }
//
//    completed.await();
//
//    long meanLatency = calculateMeanLatency(latencies);
//    long medianLatency = calculateMedianLatency(latencies);
//    long p99Latency = calculatePercentileLatency(latencies, 99);
//    long minLatency = Collections.min(latencies);
//    long maxLatency = Collections.max(latencies);
//    long endTime = System.currentTimeMillis();
//    long wallTime = endTime - startTime;
//    long throughput = NUM_REQUESTS * NUM_THREADS / wallTime * 1000;
//
//    System.out.println("Mean Latency (ms): " + meanLatency);
//    System.out.println("Median Latency (ms): " + medianLatency);
//    System.out.println("99th Percentile Latency (ms): " + p99Latency);
//    System.out.println("Min Latency (ms): " + minLatency);
//    System.out.println("Max Latency (ms): " + maxLatency);
//    System.out.println("Throughput (requests/s): " + throughput);
//  }
//
//  private static long calculateMeanLatency(List<Long> latencies) {
//    long sum = 0;
//    for (long latency : latencies) {
//      sum += latency;
//    }
//    return sum / latencies.size();
//  }
//
//  private static long calculateMedianLatency(List<Long> latencies) {
//    Collections.sort(latencies);
//    int midIndex = latencies.size() / 2;
//    if (latencies.size() % 2 == 0) {
//      return (latencies.get(midIndex - 1) + latencies.get(midIndex)) / 2;
//    }
//    return latencies.get(midIndex);
//  }
//
//  private static long calculatePercentileLatency(List<Long> latencies, int percentile) {
//    Collections.sort(latencies);
//    int index = (int) Math.ceil((double) latencies.size() * percentile / 100) - 1;
//    return latencies.get(index);
//  }
//}

