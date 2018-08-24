package ch09.apiserver;

import java.net.InetSocketAddress;

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

//ApiServer의 객체가 스프링 컨텍스트에 등록되게 함
@Component
public class ApiServer {
	
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
			channelFuture.sync();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
		finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
}
