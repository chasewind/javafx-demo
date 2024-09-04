package com.example.javafxdemo;

import com.example.javafxdemo.event.DefaultEventBus;
import com.example.javafxdemo.event.Event;
import com.example.javafxdemo.event.EventType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Slf4j
public class RemoteCallApi {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static ResponseResult<String> queryDsl(String basicUrl, String authHead,String method,
                                                       String indexName,String action,
                                                       String requestJson){
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        if(!basicUrl.endsWith("/")){
            basicUrl = basicUrl+"/";
        }
        if(StringUtils.isNotEmpty(action) ){
            builder.uri(URI.create(basicUrl+indexName+"/"+action));
        }else{
            builder.uri(URI.create(basicUrl+indexName));
        }

        if(StringUtils.isNotEmpty(authHead)){
            builder.header("Authorization", authHead);
        }
        //observableArrayList("GET", "POST", "PUT", "HEAD", "DELETE");
        switch (method){
            case "POST":
                builder.header("Content-Type", "application/json");
                builder.POST(HttpRequest.BodyPublishers.ofString(requestJson));
                break;
            case "PUT":
                builder.header("Content-Type", "application/json");
                builder.PUT(HttpRequest.BodyPublishers.ofString(requestJson));
                break;
            case "HEAD":
                log.info("ignore HEAD Request");
                break;
            case "DELETE":
                log.info("ignore DELETE Request");
                break;
            default:
                builder.GET();
                break;

        }

        return sendRequest(builder.build());
    }

    public static HttpRequest buildHttpRequestWithAuth(String basicUrl, String authHead) {
        if (StringUtils.isNotEmpty(authHead)) {
            return
                    HttpRequest.newBuilder(URI.create(basicUrl)).header(
                            "Authorization", authHead).build();
        } else {
            return
                    HttpRequest.newBuilder(URI.create(basicUrl)).build();
        }

    }
    public static ResponseResult<String> loginWithAuthHead(String basicUrl, String authHead){

        HttpRequest httpRequest =  buildHttpRequestWithAuth(basicUrl,authHead);
        return sendRequest(httpRequest);
    }

    public static ResponseResult<String> loginWithBaseUrl(String basicUrl){
        HttpRequest httpRequest =  buildHttpRequestWithAuth(basicUrl,null);
        return sendRequest(httpRequest);
    }
    public static ResponseResult<String> loginWithUsernameAndPassword(String basicUrl,String username,String password){
        HttpRequest httpRequest =  buildHttpRequestWithUsernameAndPassword(basicUrl,username,password);
        return sendRequest(httpRequest);
    }
    public static HttpRequest buildHttpRequestWithUsernameAndPassword(String basicUrl,String username,String password){
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        String authHeader = "Basic " + encodedAuth;
        return
                HttpRequest.newBuilder(URI.create(basicUrl)).header(
                        "Authorization", authHeader).GET().build();
    }

    public static ResponseResult<String> sendRequest(HttpRequest httpRequest) {
        ResponseResult<String> responseResult = new ResponseResult<>();
        try {
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseResult.setHttpStatus(response.statusCode());
            responseResult.setData(response.body());
        } catch (Exception e) {
            Event<String> event = new Event<>(EventType.CONNECT_FAIL_CLUSTER, e.getMessage());
            DefaultEventBus.getInstance().sendEvent(event);
        }
        return responseResult;

    }


}
