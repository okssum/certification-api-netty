package ch09.apiserver.service;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ch09.apiserver.core.ApiRequestTemplate;

// 스프링 컨텍스트가  UserInfo 클래스를 생성할 수 있도록 해줌. users는 객체 이름
// 즉 getBean 메서드 호출 인자로 사용됨
@Service("users")
// 스프링 컨텍스트가 객체를 생성할 때 싱글톤으로 생성할 것인지 객체를 요청할 때마다 새로 생성할 것인지 설정함
// 여기서 설정된 prototype 값은 요청할 때마다 새로 생성한다는 의미. 이 어노테이션을 지정하지 않으면 싱글톤으로 생성됨
@Scope("prototype")
public class UserInfo extends ApiRequestTemplate {
	
	// 앞에서 설정한 HSQLDB와 마이바티스 스프링 설정을 기초로 하여 마이바티스의 sqlSession 객체를 생성하여 할당함
	@Autowired
	private SqlSession sqlSession;
	
	public UserInfo(Map<String, String> reqData) {
		super(reqData);
	}
	
	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(this.reqData.get("email"))) {
			throw new RequestParamException("email이 없습니다.");
		}
	}
	
	@Override
	public void service() throws ServiceException {
		Map<String, Object> result = sqlSession.selectOne("users.userInfoByEmail", this.reqData);
		
		if (result != null) {
			String userNo = String.valueOf(result.get("USERNO"));
			
			this.apiResult.addProperty("resultCode", "200");
			this.apiResult.addProperty("message", "Success");
			this.apiResult.addProperty("userNo", userNo);
		}
		else {
			this.apiResult.addProperty("resultCode", "404");
			this.apiResult.addProperty("message", "Fail");
		}
	}
	
}
