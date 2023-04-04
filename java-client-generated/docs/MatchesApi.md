# MatchesApi

All URIs are relative to *https://virtserver.swaggerhub.com/IGORTON/Twinder/1.2.1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**matches**](MatchesApi.md#matches) | **GET** /matches/{userID}/ | 

<a name="matches"></a>
# **matches**
> Matches matches(userID)



return a maximum of 100 potential matches for a user

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.MatchesApi;


MatchesApi apiInstance = new MatchesApi();
String userID = "userID_example"; // String | user to return matches for
try {
    Matches result = apiInstance.matches(userID);
    System.out.println(result);
} catch (ApiException e) {
    System.err.println("Exception when calling MatchesApi#matches");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **userID** | **String**| user to return matches for |

### Return type

[**Matches**](Matches.md)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: application/json

