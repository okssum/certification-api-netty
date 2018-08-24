package ch09.apiserver.core;

import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch09.apiserver.service.ServiceException;

@Service("notFound")
@Scope("prototype")
public class DefaultApiRequest extends ApiRequestTemplate {
	
	public DefaultApiRequest(Map<String, String> reqData) {
		super(reqData);
	}
	
	@Override
	public void service() throws ServiceException {
		this.apiResult.addProperty("resultCode", "404");
	}
	
}
