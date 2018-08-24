package ch09.apiserver.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import ch09.apiserver.service.RequestParamException;
import ch09.apiserver.service.ServiceException;

/**
 * @author  sunok
 * @Comment ApiRequest 인터페이스 중에서 executeService, getApiResult 메서드만 구현함
 * 			아직 구현하지 않은 requestParamValidation, service 메서드는 ApiRequestTemplate 클래스를 상속받은 클래스에서 구현
 * 			executeService()를 호출하면 서비스에 따라서 다른 로직을 수행할 수 있음
 */
public abstract class ApiRequestTemplate implements ApiRequest {
	
	protected Logger logger;
	
	protected Map<String, String> reqData;
	
	protected JsonObject apiResult;
	
	public ApiRequestTemplate(Map<String, String> reqData) {
		this.logger = LogManager.getLogger(this.getClass());
		this.apiResult = new JsonObject();
		this.reqData = reqData;
		
		logger.info("request data : " + this.reqData); 
	}
	
	@Override
	public void executeService() {
		try {
			this.requestParamValidation();
			
			this.service();
		}
		catch (RequestParamException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "405");
		}
		catch (ServiceException e) {
			logger.error(e);
			this.apiResult.addProperty("resultCode", "501");
		}
	}
	
	@Override
	public JsonObject getApiResult() {
		return this.apiResult;
	}
	
	@Override
    public void requestParamValidation() throws RequestParamException {
        if (getClass().getClasses().length == 0) {
            return;
        }

        // // TODO 이건 꼼수 바꿔야 하는데..
        // for (Object item :
        // this.getClass().getClasses()[0].getEnumConstants()) {
        // RequestParam param = (RequestParam) item;
        // if (param.isMandatory() && this.reqData.get(param.toString()) ==
        // null) {
        // throw new RequestParamException(item.toString() +
        // " is not present in request param.");
        // }
        // }
    }

//    public final <T extends Enum<T>> T fromValue(Class<T> paramClass, String paramValue) {
//        if (paramValue == null || paramClass == null) {
//            throw new IllegalArgumentException("There is no value with name '" + paramValue + " in Enum "
//                    + paramClass.getClass().getName());
//        }
//
//        for (T param : paramClass.getEnumConstants()) {
//            if (paramValue.equals(param.toString())) {
//                return param;
//            }
//        }
//
//        throw new IllegalArgumentException("There is no value with name '" + paramValue + " in Enum "
//                + paramClass.getClass().getName());
//    }
	
}
