# StatsApi

All URIs are relative to *https://virtserver.swaggerhub.com/IGORTON/Twinder/1.2.1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**matchStats**](StatsApi.md#matchStats) | **GET** /stats/{userID}/ | 

<a name="matchStats"></a>
# **matchStats**
> MatchStats matchStats(userID)



return number of likes and dislikes for a user

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.StatsApi;


StatsApi apiInstance = new StatsApi();
String userID = "userID_example"; // String | user to return stats for
try {
    MatchStats result = apiInstance.matchStats(userID);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling StatsApi#matchStats");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userID** | **String**| user to return stats for |

### Return type

[**MatchStats**](MatchStats.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

