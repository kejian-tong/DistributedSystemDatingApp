package CalculateLatency;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.MatchesApi;
import io.swagger.client.api.StatsApi;
import io.swagger.client.model.MatchStats;
import io.swagger.client.model.Matches;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetThread extends Thread implements Runnable {

  private LatencyStats latencyStats;
  private final Random random = new Random();
  private String userID = String.valueOf(ThreadLocalRandom.current().nextInt(1, 5001));
  private AtomicBoolean running;
  private final int maxAttempts = 5;

  public GetThread(LatencyStats latencyStats, AtomicBoolean running) {
    this.latencyStats = latencyStats;
    this.running = running;
  }

  @Override
  public void run() {
    MatchesApi matchesApi = new MatchesApi(new ApiClient());
    StatsApi statsApi = new StatsApi(new ApiClient());

    int connectionTimeout = 300; // 3 seconds
    int readTimeout = 300; // 3 seconds

    matchesApi.getApiClient().setConnectTimeout(connectionTimeout);
    matchesApi.getApiClient().setReadTimeout(readTimeout);
    matchesApi.getApiClient().setBasePath("http://35.91.149.233:8080/GetServlet_war");

    statsApi.getApiClient().setConnectTimeout(connectionTimeout);
    statsApi.getApiClient().setReadTimeout(readTimeout);
    statsApi.getApiClient().setBasePath("http://35.91.149.233:8080/GetServlet_war");

    while (running.get()) {
      for (int i = 0; i < 5; i++) {
        long start = System.currentTimeMillis();
        userID = String.valueOf(ThreadLocalRandom.current().nextInt(1, 5001));
        boolean success = false;
        int attempts = 0;

        while (!success && attempts < maxAttempts) {
          try {
            if (random.nextBoolean()) {
              ApiResponse<Matches> res = matchesApi.matchesWithHttpInfo(userID);
            } else {
              ApiResponse<MatchStats> response = statsApi.matchStatsWithHttpInfo(userID);
            }
            success = true;
          } catch (ApiException e) {
            System.out.println(e.getCode() + e.getMessage() + "1");
            if(e.getCode() == 400 || e.getCode() == 404) {
              System.out.println(e.getCode() + e.getMessage() + "2");
              success = true;
            } else {
              System.out.println(e.getCode() + e.getMessage() + "3");
              attempts++;
              if (attempts >= maxAttempts) {
                throw new RuntimeException(e);
              }
            }
          }
        }

        long latency = System.currentTimeMillis() - start;
        latencyStats.record(latency);
      }
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
