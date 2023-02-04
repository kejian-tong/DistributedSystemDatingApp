package Part2;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


public class FileWriter {
  private BlockingQueue<LatencyRecord> latencyRecords;

  public FileWriter(BlockingQueue<LatencyRecord> latencyRecords) {
    this.latencyRecords = latencyRecords;
  }

  public void run() throws IOException {
    String resultFile = "results.csv";

    java.io.FileWriter writer = new java.io.FileWriter(resultFile);

    writer.write("StartTime,RequestTye,Latency,ResponseCode\n");
    for(LatencyRecord record: latencyRecords) {
      writer.write(record.getStartTime() + "," + record.getRequestType() +
          "," + record.getLatency() + "," + record.getResponseCode() + "\n");
    }

    writer.flush();
    writer.close();
  }
}

