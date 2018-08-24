package ch09.apiserver.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author  sunok
 * @Comment 레디스에 접근하기 위한 제디스 헬퍼 클래스
 */
public class JedisHelper {
	
	protected static final String REDIS_HOST = "127.0.0.1";
	protected static final int REDIS_PORT = 6379;
	private final Set<Jedis> connectionList = new HashSet<>();
	private final JedisPool pool;
	
	// 싱글톤으로 작성. 외부에서 생성자를 호출할 수 없도록 private 접근 지정자 사용
	private JedisHelper() {
		// 제디스의 커넥션 풀을 사용하기 위해 아파치 commons pool2 라이브러리 사용
		// 레디스는 API 서버와 같은 운영체제에서 수행된다고 가정
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxTotal(20);
		config.setBlockWhenExhausted(true);
		
		this.pool = new JedisPool(config, REDIS_HOST, REDIS_PORT);
	}
	
	private static class LazyHolder {
		// 내부 클래스로부터의 최적화되지 않은 액세스와 관련된 경고를 억제
		@SuppressWarnings("synthetic-access")
		private static final JedisHelper INSTANCE = new JedisHelper();
	}
	
	@SuppressWarnings("synthetic-access")
	public static JedisHelper getInstance() {
		return LazyHolder.INSTANCE;
	}
	
	// 싱글톤 객체에 접근하는 외부 메서드
	final public Jedis getConnection() {
		Jedis jedis = this.pool.getResource();
		this.connectionList.add(jedis);
		
		return jedis;
	}
	
	final public void returnResource(Jedis jedis) {
		this.pool.returnResource(jedis);
	}
	
	final public void destoryPool() {
		Iterator<Jedis> jedisList = this.connectionList.iterator();
		while (jedisList.hasNext()) {
			Jedis jedis = jedisList.next();
			this.pool.returnResource(jedis);
		}
		
		this.pool.destroy();
	}
	
}
