package part2latency;

public class Record {
  private long startTime;
  private RequestType requestType;
  private int latency;   // unit: ms
  private int responseCode;

  private String senderThreadName;  // sent by which thread

  public Record(long startTime, RequestType requestType, int latency, int responseCode, String senderThreadName) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
    this.senderThreadName = senderThreadName;
  }

  public long getStartTime() {
    return startTime;
  }

  public RequestType getRequestType() {
    return requestType;
  }

  public int getLatency() {
    return latency;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public String getSenderThreadName() {
    return senderThreadName;
  }

  @Override
  public String toString() {
    return startTime +
        "," + requestType +
        "," + latency +
        "," + responseCode +
        "," + senderThreadName
        ;
  }
}
