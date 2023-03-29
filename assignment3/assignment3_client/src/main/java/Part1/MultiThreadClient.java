package Part1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {
  private static final int numOfThreads = 110;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger();
  private static AtomicInteger numOfFailReq = new AtomicInteger();
  private static final int totalRequest = 200000;

  public static void main(String[] args) throws InterruptedException {
    long start = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    for(int i = 0; i < numOfThreads; i++) {
      int numOfRequest = totalRequest / numOfThreads;
      if (i == numOfThreads - 1 ) {
        numOfRequest += totalRequest % numOfThreads;
      }
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfRequest, completed);
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
    long throughput = (numOfRequests) / walltime;

    System.out.println("Number of successful requests: " + numOfSuccessReq);
    System.out.println("Number of fail requests: " + numOfFailReq);
    System.out.println("walltime: " + walltime + " seconds");
    System.out.println("Total throughput: " + throughput + " req/s");

  }
}
