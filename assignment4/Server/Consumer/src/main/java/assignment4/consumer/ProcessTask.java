package assignment4.consumer;

import com.mongodb.client.MongoClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ProcessTask implements Runnable {
  private final List<ConsumerRecord<String, String>> records;
  private final TopicPartition partition;

  // volatile: visible to other threads (i.e. ConsumerThread),
  // but doesn't need to be atomic.(because these variables will only be modified
  // in this thread)
  private volatile boolean stopped = false;

  private volatile boolean started = false;

  private volatile boolean finished = false;

  private final CompletableFuture<Long> completionFuture = new CompletableFuture<>();

  private final ReentrantLock startStopLock = new ReentrantLock();

  private final AtomicLong currentOffset = new AtomicLong();

  private Logger log = LoggerFactory.getLogger(ProcessTask.class);

  private MongoClient mongoClient;


  public ProcessTask(TopicPartition partition, List<ConsumerRecord<String, String>> records,
       MongoClient mongoClient) {
    this.records = records;
    this.partition = partition;
    this.mongoClient = mongoClient;
  }

  @Override
  public void run() {
    // A ProcessTask might be stopped after the thread starts to run
    // but before it starts processing records.
    // In this case, directly exit this method, leaving currentOffset() to be the initial value: 0

    System.out.println("ProcessTask: " + this + " starts process partition: " + partition + " :" + records.size() + ""
        + "in Thread: " + Thread.currentThread().getName());
    this.startStopLock.lock();
    if (this.stopped){
      return;
    }
    this.started = true;
    this.startStopLock.unlock();

    boolean reachBatchSize = true;
    ConsumerRecord<String, String> curRecord = null;
    System.out.println("ProcessTask: " + this + " starts looping partition: " + partition + " :" + records.size() + ""
        + "in Thread: " + Thread.currentThread().getName());

    int record_cnt = 0;
    for (ConsumerRecord<String, String> record : records) {
      record_cnt ++;
      if (this.stopped)
        break;      // Ensure that as soon as this task is stopped by ConsumerThread, it will complete the completionFuture with the current offset.
      // process record here and make sure you catch all exceptions

      // The multi-threaded solution here allows us to take as much time as needed to process a record,
      // so we can simply retry processing in a loop until it succeeds.
      curRecord = record;
      // Accumulate 60 messages,then batch write & set offset.
      this.putToBatchMap(record);
      System.out.println("ProcessTask: " + this + "has put message No." + record_cnt + "to map in Thread:" + Thread.currentThread().getName());
      // batchUpdateToDB will keep trying bulkWrite until succeed
      reachBatchSize = this.batchUpdateToDB(false);  // not force

      if (reachBatchSize) {
        this.resetBatchMap();
        this.currentOffset.set(record.offset() + 1);    // Set the currentOffset to the last processed record's offset.
        System.out.println("ProcessTask: " + this + "finished setting offset for current batch.");
      }
      // If hasn't reach the batch size, then no writes to DB, and no commit offset set for Kafka broker.
    }

    // if the last batch is not accumulated to 60, do another FORCE write & set offset.
    if (!reachBatchSize) {
      this.batchUpdateToDB(true);
      this.resetBatchMap();
      this.currentOffset.set(curRecord.offset() + 1);
      System.out.println("ProcessTask: " + this + "finished setting offset for LAST REMAINING batch.");
    }

    this.finished = true;
    this.completionFuture.complete(this.currentOffset.get());
    System.out.println("ProcessTask: " + this + "FINISHED !!");
  }

  public long getCurrentOffset() {
    return this.currentOffset.get();
  }

  public void stop() {
    this.startStopLock.lock();
    this.stopped = true;
    // Similar to the beginning of run() method,
    // this thread might be stopped before it even starts processing the records (i.e. this.started == false)
    // so in this case we need to properly set the "finished" flag and complete the future with currentOffset(i.e. initial value == 0)
    if (!this.started) {
      this.finished = true;
      this.completionFuture.complete(this.currentOffset.get());
    }
    this.startStopLock.unlock();
  }

  public long waitForCompletion() {
    try {
      return this.completionFuture.get();  // will block until the future is completed.
    } catch (InterruptedException | ExecutionException e) {
      return -1;  // TODO: ExecutionException: might need to log, to see what's wrong on DB side.
    }
  }

  public boolean isFinished() {
    return this.finished;
  }


  protected abstract void putToBatchMap(ConsumerRecord<String, String> record);

  /**
   * If the current batch size reaches the CONSUMER_BATCH_UPDATE_SIZE, do batch update and return true;
   * Otherwise, do nothing and return false
   */
  protected abstract boolean batchUpdateToDB(boolean force);


  protected abstract void resetBatchMap();



}
