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
    double mean = (double) totalLatency.sum() / count.sum();

    System.out.println("Get Min latency: " + min + " ms");
    System.out.println("Get Mean latency: " + mean + " ms");
    System.out.println("Get Max latency: " + max + " ms");
  }
}
