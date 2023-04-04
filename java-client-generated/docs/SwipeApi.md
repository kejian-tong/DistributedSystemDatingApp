# SwipeApi

All URIs are relative to *https://virtserver.swaggerhub.com/IGORTON/Twinder/1.2.1*

Method | HTTP request | Description
------------- | ------------- | -------------
[**swipe**](SwipeApi.md#swipe) | **POST** /swipe/{leftorright}/ | 

<a name="swipe"></a>
# **swipe**
> swipe(body, leftorright)



Swipe left or right

### Example
```java
// Import classes:
//import io.swagger.client.ApiException;
//import io.swagger.client.api.SwipeApi;


SwipeApi apiInstance = new SwipeApi();
SwipeDetails body = new SwipeDetails(); // SwipeDetails | response details
String leftorright = "leftorright_example"; // String | value should be \"left\" or \"right\"
try {
    apiInstance.swipe(body, leftorright);
} catch (ApiException e) {
    System.err.println("Exception when calling SwipeApi#swipe");
    e.printStackTrace();
}
```

### Parameters

Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **body** | [**SwipeDetails**](SwipeDetails.md)| response details |
 **leftorright** | **String**| value should be \&quot;left\&quot; or \&quot;right\&quot; |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: application/json

