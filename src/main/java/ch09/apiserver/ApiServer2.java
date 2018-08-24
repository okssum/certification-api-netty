package ch09.apiserver;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.SelfSignedCertificate;

/**
 * @author  2beone
 * @Comment HTTP, HTTPS 요청을 하나의 API 서버에서 처리하기
 */
//ApiServer2의 객체가 스프링 컨텍스트에 등록되게 함
@Component
public class ApiServer2 {
	
	// ApiServerConfig 클래스에서 프로퍼티 파일을 참조하여 생성된 InetSocketAddress 객체를
	// 스프링의 @Autowired 사용하여 자동으로 할당
	@Autowired
	@Qualifier("tcpSocketAddress")
	private InetSocketAddress address;
	
	@Autowired
	@Qualifier("workerThreadCount")
	private int workerThreadCount;
	
	@Autowired
	@Qualifier("bossThreadCount")
	private int bossThreadCount;
	
	public void start() {
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossThreadCount);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workerThreadCount);
		ChannelFuture channelFuture = null;
		
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			 .channel(NioServerSocketChannel.class)
			 .handler(new LoggingHandler(LogLevel.INFO))
			 // API 서버의 채널 파이프라인 설정 클래스 지정
			 // ApiServerInitializer 인자는 SSL 컨텍스트임
			 .childHandler(new ApiServerInitializer(null));
			
			Channel ch = b.bind(address).sync().channel();
			
			channelFuture = ch.closeFuture();
			// sync() 호출하면 코드가 블로킹되어 이후의 코드가 실행되지 않으므로 주석으로 처리함
//			channelFuture.sync();
			
			final SslContext sslCtx;
			// SSL 연결 지원
			// SelfSignedCertificate 클래스는 자기 스스로 서명한 인증서를 생성하므로 일반 브라우저에서 접속하면 경고 메시지가 출력됨
			SelfSignedCertificate ssc = new SelfSignedCertificate();
			sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
			
			// 새로운 부트스트랩 추가
			ServerBootstrap b2 = new ServerBootstrap();
			// 이벤트 루프는 첫번째 부트스트랩과 공유하여 사용하도록 설정
			b2.group(bossGroup, workerGroup)
			 .channel(NioServerSocketChannel.class)
			 .handler(new LoggingHandler(LogLevel.INFO))
			 // ApiServerInitializer를 같이 사용하게 되어 SSL 포트로 접근하더라도 동일한 로직을 처리할 수 있음
			 .childHandler(new ApiServerInitializer(sslCtx));
			
			// 두 개의 포트 사용
			Channel ch2 = b2.bind(8443).sync().channel();
			
			channelFuture = ch2.close();
			channelFuture.sync();
		}
		catch (InterruptedException | CertificateException e) {
			e.printStackTrace();
		}
		catch (SSLException e) {
			e.printStackTrace();
		}
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
}
