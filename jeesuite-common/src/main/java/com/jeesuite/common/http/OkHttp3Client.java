package com.jeesuite.common.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.jeesuite.common.http.HttpRequestEntity.FileItem;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 
 * 
 * <br>
 * Class Name   : OkHttp3Client
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Apr 29, 2021
 */
public class OkHttp3Client implements HttpClientProvider{

	private static Map<String, MediaType> mediaTypeMappings = new HashMap<>();
	private static OkHttpClient httpClient;
	
	static {
		mediaTypeMappings.put(CONTENT_TYPE_JSON_UTF8, MediaType.parse(CONTENT_TYPE_JSON_UTF8));
		mediaTypeMappings.put(CONTENT_TYPE_FROM_URLENCODED_UTF8, MediaType.parse(CONTENT_TYPE_FROM_URLENCODED_UTF8));
		//
		httpClient = new OkHttpClient.Builder().connectionPool(new ConnectionPool(1, 60L, TimeUnit.SECONDS))
				.connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).readTimeout(readTimeout, TimeUnit.MILLISECONDS)
				.writeTimeout(readTimeout, TimeUnit.MILLISECONDS).build();
	}
	
	@Override
	public HttpResponseEntity execute(String uri, HttpRequestEntity requestEntity) throws IOException {
		
		HttpUrl.Builder urlBuilder = HttpUrl.parse(uri).newBuilder();
		if (requestEntity.getQueryParams() != null) {
			for (String key : requestEntity.getQueryParams().keySet()) {
				urlBuilder.addQueryParameter(key, requestEntity.getQueryParams().get(key).toString());
			}
		}
		
		okhttp3.Headers.Builder headerBuilder = new Headers.Builder();
		if (requestEntity.getHeaders() != null) {
			for (String key : requestEntity.getHeaders().keySet()) {
				headerBuilder.add(key, requestEntity.getHeaders().get(key).toString());
			}
		}
		
		if(requestEntity.getBasicAuth() != null) {
			headerBuilder.add("Authorization", requestEntity.getBasicAuth().getEncodeBasicAuth());
		}
		
		okhttp3.Request.Builder requestBuilder = new Request.Builder().headers(headerBuilder.build())
				.url(urlBuilder.build());
		
		if(HttpMethod.POST == requestEntity.getMethod()) {
			RequestBody body = null;
			if(requestEntity.getBody() != null) {
				body = FormBody.create(contentType2MediaType(requestEntity.getContentType()), requestEntity.getBody());
			} else if(requestEntity.getFormParams() != null) {
				Set<Entry<String, Object>> formEntries = requestEntity.getFormParams().entrySet();
				Object entryValue;
				if(requestEntity.isMultipart()) {
					MultipartBody.Builder builder = new MultipartBody.Builder();
					for (Entry<String, Object> entry : formEntries) {
						entryValue = entry.getValue();
						if(entryValue == null)continue;
						if(entryValue instanceof FileItem) {
							FileItem fileItem = (FileItem)entryValue;
							builder.addFormDataPart(entry.getKey(), fileItem.getFileName(), RequestBody.create(fileItem.getContent(), null));
						}else {
							builder.addFormDataPart(entry.getKey(), entryValue.toString());
						}
					}
					body = builder.build();
				}else {
					FormBody.Builder builder = new FormBody.Builder();
					for (Entry<String, Object> entry : formEntries) {
						if(entry.getValue() == null)continue;
						builder.add(entry.getKey(), entry.getValue().toString());
					}
					body = builder.build();
				}
			}
			
			if(body != null) {
				requestBuilder.post(body);
			}
		}
		
		HttpResponseEntity responseEntity = new HttpResponseEntity();
		
		Response response = httpClient.newCall(requestBuilder.build()).execute();
		responseEntity.setStatusCode(response.code());
		if (response.body() != null) {
			responseEntity.setBody(response.body().string());
		}
		if (!response.isSuccessful()) {
			responseEntity.setMessage(response.message());
		}
		
		return responseEntity;
	}
	
	private static MediaType contentType2MediaType(String contentType) {
		if(mediaTypeMappings.containsKey(contentType)) {
			return mediaTypeMappings.get(contentType);
		}
		return MediaType.parse(CONTENT_TYPE_FROM_URLENCODED_UTF8);
	}

}
