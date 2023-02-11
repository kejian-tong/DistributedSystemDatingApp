package Part1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {
  private static final int numOfThreads = 500;
  private static final int numOfReq = 1000;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger();
  private static AtomicInteger numOfFailReq = new AtomicInteger();

  public static void main(String[] args) throws InterruptedException {
    long start = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    for(int i = 0; i < numOfThreads; i++) {
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfReq, completed);
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
