package ch09.apiserver;

import ch09.apiserver.core.ApiRequestParser;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;

public class ApiServerInitializer extends ChannelInitializer<SocketChannel> {
	
	// 채널 보안을 위한 SSL 컨텍스트 객체 지정
	private final SslContext sslCtx;
	
	public ApiServerInitializer(SslContext sslCtx) {
		this.sslCtx = sslCtx;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		
		// 클라이언트 채널로 수신된 HTTP 데이터를 처리하기 위한 채널 파이프라인 객체를 생성
		ChannelPipeline p = ch.pipeline();
		if (sslCtx != null) {
			p.addLast(sslCtx.newHandler(ch.alloc()));
		}
		
		/**
		 * 클라이언트로부터 데이터를 수신했을 때 데이터 핸들러는 1, 2, 4, 5 순으로 호출됨
		 * ApiRequestParser 처리가 완료되어 채널로 데이터를 기록할 때 호출되는 데이터 핸들러는 5, 4, 3 순서로 호출됨
		 */
		// HTTP 요청을 처리하는 디코더
		// 즉 클라이언트가 전송한 HTTP 프로토콜을 네티의 바이트 버퍼로 변환하는 작업을 수행함
		p.addLast(new HttpRequestDecoder()); // 1
		// 2
		// HTTP 프로토콜에서 발생하는 메시지 파편화를 하나로 합쳐 처리하는 디코더
		// 인자 - 한꺼번에 처리 가능한 최대 데이터 크기. 65Kbyte 이상의 데이터가 하나의 HTTP 요청으로 수신되면 TooLongFrameExcepton 예외가 발생
		p.addLast(new HttpObjectAggregator(65536)); // 2
		// 수신된 HTTP 요청의 처리 결과를 클라이언트로 전송할 때 HTTP 프로토콜로 변환해주는 인코더
		p.addLast(new HttpResponseEncoder()); // 3
		// HTTP 프로토콜로 송수신되는 HTTP의 본문 데이터를 gzip 압출 알고리즘을 사용하여 압축과 압축 해제를 수행
		// 즉 HttpContentCompressor는 ChannelDuplexHandler 클래스를 상속받기 때문에 인바운드와 아웃바운드에서 모두 호출
		p.addLast(new HttpContentCompressor()); // 4
		// ApiRequestParser - 클라이언트로부터 수신된 HTTP 데이터에서 헤더, 데이터 값을 추출하여 토큰 발급과 같은 업무 처리 클래스로 분기하는 클래스
		// API 서버의 컨트롤러 역활을 수행
		p.addLast(new ApiRequestParser()); // 5
	}
	
}
