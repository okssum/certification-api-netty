package ch09.apiserver;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

// 지정된 클래스가 스프링의 설정 정보를 포함한 클래스임을 표시
@Configuration
// @Configuration 클래스에서 XML 설정 정보를 함께 사용하고 싶다면 @ImportResource 어노테이션을 사용하면 됨
@ImportResource("classpath:spring/hsqlApplicationContext.xml")
// 스프링이 컴포넌트를 검색할 위치를 지정
// 여기서는 ApiServer 클래스가 위치한 ch09.apiserver와 토큰 발급 및 사용자 정보 클래스가 위치한 ch09.apiserver.service 패키지를 지정함
@ComponentScan("ch09.apiserver, ch09.apiserver.core, ch09.apiserver.service")
//설정 정보를 가진 파일의 위치에서 파일을 읽어서 Environment 객체로 자동 저장
@PropertySource("classpath:api-server.properties")
public class ApiServerConfig {
	
	@Value("${boss.thread.count}")
	private int bossThreadCount;
	
	@Value("${worker.thread.count}")
	private int workerThreadCount;
	
	@Value("${tcp.port}")
	private int tcpPort;
	
	// 프로퍼티에서 읽어들인 boss.thread.count 값을 @Bean 사용하여 다른 클래스에서 참조할 수 있도록 설정
	// 이 값은 ApiServer 클래스의 부트스트랩에서 사용됨
	@Bean(name = "bossThreadCount")
	public int getBossThreadCount() {
		return bossThreadCount;
	}
	
	// 이 값은 ApiServer 클래스의 부트스트랩에서 사용됨
	@Bean(name = "workerThreadCount")
	public int getWorkerThreadCount() {
		return workerThreadCount;
	}

	public int getTcpPort() {
		return tcpPort;
	}
	
	// 객체 이름을 tcpSocketAddress로 지정
	// 이 설정은 스프링 컨텍스트에 tcpSocketAddress라는 이름으로 추가되며 다른 Bean에서 사욯할 수 있음
	@Bean(name = "tcpSocketAddress")
	public InetSocketAddress tcpPort() {
		return new InetSocketAddress(tcpPort);
	}
	
	// @PropertySource에서 사용할 Environment 객체를 생성하는 Bean을 생성
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}
	
}
