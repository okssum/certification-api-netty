package ch09.apiserver.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author  sunok
 * @Comment ApiRequestParser 클래스에서 추출한 HTTP 요청 데이터를 기준으로 그에 해당하는 API 서비스 클래스를 생성하여 돌려줌
 */
@Component
public class ServiceDispatcher {
	
	// 정적 변수에 스프링 컨텍스트 할당
	private static ApplicationContext springContext;
	
	// 스프링 컨텍스트는 정적 변수에 직접 할당할 수 없기 때문에 메서드에  @Autowired을 사용하여 간접적으로 할당
	@Autowired
	public void init(ApplicationContext springContext) {
		ServiceDispatcher.springContext = springContext;
	}
	
	protected Logger logger = LogManager.getLogger(this.getClass());
	
	public static ApiRequest dispatch(Map<String, String> requestMap) {
		String serviceUri = requestMap.get("REQUEST_URI");
		String beanName = null;
		
		if (serviceUri == null) {
			beanName = "notFound";
		}
		
		if (serviceUri.startsWith("/tokens")) {
			String httpMethod = requestMap.get("REQUEST_METHOD");
			
			switch (httpMethod) {
			case "POST":
				beanName = "tokenIssue";
				break;
			case "DELETE":
				beanName = "tokenExpier";
				break;
			case "GET":
				beanName = "tokenVerify";
				break;
				
			default:
				beanName = "notFound";
				break;
			}
		}
		else if (serviceUri.startsWith("/users")) {
			beanName = "users";
		}
		else {
			beanName = "notFound";
		}
		
		ApiRequest service = null;
		try {
			// beanName 값을 이용하여 스프링 컨텍스트에 API 서비스 클래스 객체를 생성함
			service = (ApiRequest) springContext.getBean(beanName, requestMap);
		}
		catch (Exception e) {
			e.printStackTrace();
			service = (ApiRequest) springContext.getBean("notFound", requestMap);
		}
		
		return service;
	}
	
}
