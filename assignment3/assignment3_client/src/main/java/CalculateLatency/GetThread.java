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

public class GetThread implements Runnable {

  private LatencyStats latencyStats;
  private final Random random = new Random();
  private String userID = String.valueOf(ThreadLocalRandom.current().nextInt(1, 50001));
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
    matchesApi.getApiClient().setBasePath("http://52.24.58.125:8080/assignment4_server_war");
//    matchesApi.getApiClient().setBasePath("http://localhost:8080/assignment4_server_war_exploded");

    statsApi.getApiClient().setConnectTimeout(connectionTimeout);
    statsApi.getApiClient().setReadTimeout(readTimeout);
    statsApi.getApiClient().setBasePath("http://52.24.58.125:8080/assignment4_server_war");
//    statsApi.getApiClient().setBasePath("http://localhost:8080/assignment4_server_war_exploded");

    long lastRequestTime = 0;
    long batchStartTime = System.currentTimeMillis();

    while (running.get()) {
      for (int i = 0; i < 5; i++) {
        long start = System.currentTimeMillis();
        userID = String.valueOf(ThreadLocalRandom.current().nextInt(1, 50001));
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
            if(e.getCode() == 400 || e.getCode() == 404) {
              success = true;
            } else {
              attempts++;
              if (attempts >= maxAttempts) {
                throw new RuntimeException(e);
              }
            }
          }
        }

        long requestTime = System.currentTimeMillis() - start;
        latencyStats.record(requestTime);

        long timeSinceLastRequest = System.currentTimeMillis() - lastRequestTime;
        long sleepTime = Math.max(0, 200 - timeSinceLastRequest); // 1000ms / 5 = 200ms

        try {
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }

        lastRequestTime = System.currentTimeMillis();
      }

      long batchTime = System.currentTimeMillis() - batchStartTime;

      if (batchTime < 1000) {
        try {
          Thread.sleep(1000 - batchTime);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      batchStartTime = System.currentTimeMillis();
    }
  }
}

