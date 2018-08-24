package ch09.apiserver.core;

import com.google.gson.JsonObject;

import ch09.apiserver.service.RequestParamException;
import ch09.apiserver.service.ServiceException;

public interface ApiRequest {
	
	// API를 호출하는 HTTP 요청의 파라미터 값이 입력되었는지 검증하는 메서드
	public void requestParamValidation() throws RequestParamException;
	
	// 각 API 서비스에 따른 개별 구현 메서드
	public void service() throws ServiceException;
	
	// 서비스 API의 호출 시작 메서드
	public void executeService();
	
	// API 서비스의 처리 결과를 조회하는 메서드
	public JsonObject getApiResult();
	
}
