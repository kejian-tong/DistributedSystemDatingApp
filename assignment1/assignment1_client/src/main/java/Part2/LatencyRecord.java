package Part2;

public class LatencyRecord {
  private long startTime;
  private String requestType;
  private long latency;
  private int responseCode;

  public LatencyRecord(long startTime, String requestType, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public String getRequestType() {
    return requestType;
  }

  public long getLatency() {
    return latency;
  }

  public int getResponseCode() {
    return responseCode;
  }

  @Override
  public String toString() {
    return "LatencyRecord{" +
        "startTime=" + startTime +
        ", requestType='" + requestType + '\'' +
        ", latency=" + latency +
        ", responseCode=" + responseCode +
        '}';
  }
}

