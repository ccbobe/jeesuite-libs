/**
 * 
 */
package com.jeesuite.rest.filter.handler;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.MediaType;

import com.jeesuite.rest.filter.FilterHandler;
import com.jeesuite.rest.response.ResponseCode;
import com.jeesuite.rest.response.RestResponse;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月2日
 */
public class ResponseWrapperHandler implements FilterHandler {


	@Override
	public void processRequest(ContainerRequestContext requestContext, HttpServletRequest request,
			ResourceInfo resourceInfo) {}

	@Override
	public void processResponse(ContainerRequestContext requestContext, ContainerResponseContext responseContext,
			ResourceInfo resourceInfo) {
		MediaType mediaType = responseContext.getMediaType();
		if (mediaType != null && MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
			Object responseData = responseContext.getEntity();
			RestResponse jsonResponse;

			if (responseData instanceof RestResponse) {
				jsonResponse = (RestResponse) responseData;
			} else {
				jsonResponse = new RestResponse(ResponseCode.成功);
				jsonResponse.setData(responseData);
			}
			responseContext.setStatus(ResponseCode.成功.getCode());

			responseContext.setEntity(jsonResponse);

		}
	}

	@Override
	public int getPriority() {
		return 9;
	}

}
