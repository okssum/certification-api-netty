package ch09.apiserver.service;

import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.gson.JsonObject;

import ch09.apiserver.core.ApiRequestTemplate;
import ch09.apiserver.core.JedisHelper;
import ch09.apiserver.core.KeyMaker;
import ch09.apiserver.service.dao.TokenKey;
import redis.clients.jedis.Jedis;

//스프링 컨텍스트가  TokenIssue 클래스를 생성할 수 있도록 해줌. tokenIssue는 객체 이름
//즉 getBean 메서드 호출 인자로 사용됨
@Service("tokenIssue")
//스프링 컨텍스트가 객체를 생성할 때 싱글톤으로 생성할 것인지 객체를 요청할 때마다 새로 생성할 것인지 설정함
//여기서 설정된 prototype 값은 요청할 때마다 새로 생성한다는 의미. 이 어노테이션을 지정하지 않으면 싱글톤으로 생성됨
@Scope("prototype")
public class TokenIssue extends ApiRequestTemplate {
	
	// 레디스에 접근하기 위한 제디스 헬퍼 클래스
	private static final JedisHelper helper = JedisHelper.getInstance();
	
	@Autowired
	private SqlSession sqlSession;
	
	public TokenIssue(Map<String, String> reqData) {
		super(reqData);
	}
	
	@Override
	public void requestParamValidation() throws RequestParamException {
		if (StringUtils.isEmpty(this.reqData.get("userNo"))) {
			throw new RequestParamException("userNo이 없습니다.");
		}
		
		if (StringUtils.isEmpty(this.reqData.get("password"))) {
			throw new RequestParamException("password가 없습니다.");
		}
	}
	
	@Override
	public void service() throws ServiceException {
		Jedis jedis = null;
		try {
			Map<String, Object> result = sqlSession.selectOne("users.userInfoByPassword", this.reqData);
			
			if (result != null) {
				final long threeHour = 60 * 60 * 3;
				long issueDate = System.currentTimeMillis() / 1000;
				String email = String.valueOf(result.get("USERID"));
				
				JsonObject token = new JsonObject();
				token.addProperty("issueDate", issueDate);
				token.addProperty("expireDate", issueDate + threeHour);
				token.addProperty("email", email);
				token.addProperty("userNo", reqData.get("userNo"));
				
				// token 저장
				// 발급된 토큰을 레디스에 저장하고 조회하고자 KeyMaker 인터페이스를 사용함
				KeyMaker tokenKey = new TokenKey(email, issueDate);
				jedis = helper.getConnection();
				// setex 메서드 - 지정된 시간 이후에 데이터를 자동으로 삭제하는 메서드
				jedis.setex(tokenKey.getKey(), 60 * 60 * 3, token.toString());
				
				// helper
				this.apiResult.addProperty("resultCode", "200");
				this.apiResult.addProperty("message", "Success");
				this.apiResult.addProperty("token", tokenKey.getKey());
			}
			else {
				// 데이터 없음
				this.apiResult.addProperty("resultCode", "404");
			}
			
			helper.returnResource(jedis);
		}
		catch (Exception e) {
			helper.returnResource(jedis);
		}
	}
	
}
