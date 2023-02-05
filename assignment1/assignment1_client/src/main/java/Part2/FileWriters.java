package Part2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


public class FileWriters {

  public static void run(BlockingQueue<LatencyRecord> latencyRecords) throws IOException {
    String resultFile = "results.csv";
    FileWriter writer = new FileWriter(resultFile);

    writer.write("StartTime,RequestTye,Latency,ResponseCode\n");
    for(LatencyRecord record: latencyRecords) {
      writer.write(record.getStartTime() + "," + record.getRequestType() +
          "," + record.getLatency() + "," + record.getResponseCode() + "\n");
    }
    writer.flush();
    writer.close();
  }

  public static void runStats(Map<Long, Integer> requestsPerSecond) throws IOException {
    String performanceFile = "performanceCount.csv";
    FileWriter writerNew = new FileWriter(performanceFile);

    writerNew.write("Second,Requests\n");
    for(Map.Entry<Long, Integer> entry: requestsPerSecond.entrySet()) {
      writerNew.write(entry.getKey() + "," + entry.getValue() + "\n");
    }

    writerNew.flush();
    writerNew.close();
  }
}

