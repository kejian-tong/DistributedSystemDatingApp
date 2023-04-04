package CalculateLatency;

import java.util.concurrent.atomic.LongAdder;

public class LatencyStats {
  private long minLatency = Long.MAX_VALUE;
  private long maxLatency = 0;
  private LongAdder totalLatency = new LongAdder();
  private LongAdder count = new LongAdder();

  public synchronized void record(long latency) {
    minLatency = Math.min(minLatency, latency);
    maxLatency = Math.max(maxLatency, latency);
    totalLatency.add(latency);
    count.increment();
  }

  public void printStats() {
    long min = minLatency;
    long max = maxLatency;
    double mean = Math.round((double) totalLatency.sum() / count.sum());

    System.out.println("\n====== GET requests results ======\n");
    System.out.println("Min latency: " + min + " ms");
    System.out.println("Mean latency: " + mean + " ms");
    System.out.println("Max latency: " + max + " ms");
  }
}
