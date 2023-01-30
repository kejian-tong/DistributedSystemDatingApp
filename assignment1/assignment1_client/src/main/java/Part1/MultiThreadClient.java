package Part1;

import io.swagger.client.ApiClient;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiThreadClient {
  private static final int numOfThreads = 100;
  private static final int numOfReq = 5000;
  private static AtomicInteger numOfSuccessReq = new AtomicInteger(0);
  private static AtomicInteger numOfFailReq = new AtomicInteger(0);

  public void main(String[] args) throws InterruptedException {
    long before = System.currentTimeMillis();
    CountDownLatch completed = new CountDownLatch(numOfThreads);

    SingleThreadClient[] singleThreadClients = new SingleThreadClient[numOfThreads];

    for(int i = 0; i < numOfThreads; i++) {
      SingleThreadClient singleThreadClient = new SingleThreadClient(numOfReq, completed);
      Thread thread = new Thread((Runnable) singleThreadClient);
      singleThreadClients[i] = singleThreadClient;
      thread.start();
    }

    completed.await(); // wait all threads completed
    for(int i = 0; i < numOfReq; i++) {
      System.out.println(singleThreadClients[i]);
    }

    for(int i = 0; i < numOfThreads; i++) {
      System.out.println(singleThreadClients[i].getNumOfSuccessReq());
      numOfSuccessReq.addAndGet(singleThreadClients[i].getNumOfSuccessReq());
      numOfFailReq.addAndGet(singleThreadClients[i].getGetNumOfFailReq());
    }
    long after = System.currentTimeMillis();
    long walltime = ((after - before) / 1000) % 60;
    long throughput = numOfReq / walltime;

    System.out.println("Number of successful requests: " + numOfSuccessReq);
    System.out.println("Number of fail requests: " + numOfFailReq);
    System.out.println("Total throughput: " + throughput);
  }

}
