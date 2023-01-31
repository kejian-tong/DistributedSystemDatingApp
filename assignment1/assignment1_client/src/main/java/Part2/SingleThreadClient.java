package Part2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleThreadClient implements Runnable {
  private int numOfReq;
  private CountDownLatch completed;
  private List<Long> latencies;
  private AtomicInteger numOfSuccessReq;
  private AtomicInteger numOfFailReq;

  public SingleThreadClient(int numOfReq, CountDownLatch completed) {
    this.numOfReq = numOfReq;
    this.completed = completed;
    this.latencies = new ArrayList<>();
    numOfSuccessReq = new AtomicInteger();
    numOfFailReq = new AtomicInteger();
  }

  public void run() {
    try {
      for (int i = 0; i < numOfReq; i++) {
        long start = System.currentTimeMillis();
        // send POST request here and get response
        int responseCode = 0; // get response code from the response object;
        long end = System.currentTimeMillis();
        long latency = end - start;
        latencies.add(latency);
        if (responseCode == 201) {
          numOfSuccessReq.incrementAndGet();
        } else {
          numOfFailReq.incrementAndGet();
        }
      }
    } finally {
      completed.countDown();
    }
  }

  public int getNumOfSuccessReq() {
    return numOfSuccessReq.get();
  }

  public int getNumOfFailReq() {
    return numOfFailReq.get();
  }

  public List<Long> getLatencies() {
    return latencies;
  }
}
