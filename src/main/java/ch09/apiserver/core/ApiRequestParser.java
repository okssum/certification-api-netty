package ch09.apiserver.core;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

/**
 * @author  sunok
 * @Comment 인바운드 이벤트 핸들러를 상속받고 있으며 이벤트 메서드가 실행될 때 FullHttpMessage를 데이터로 받음
 * 			클라이언트가 전송한 HTTP 프로토콜 데이터는 채널 파이프라인에 등록된 HTTP 프로토콜 코덱들을 거치고 나면 FullHttpMessage 객체로 변환됨
 * 			FullHttpMessage - HttpMessage, HttpContent 인터페이스를 모두 상속함
 * 			HttpMessage - HTTP 요청과 응답을 표현하는 인터페이스. HTTP 프로코콜 버전, 요청 UR, HTTP 헤더 정보 등이 포함됨
 * 			HttpContent - HTTP 요청 프로토콜에 포함된 본문 데이터가 포함됨
 */
public class ApiRequestParser extends SimpleChannelInboundHandler<FullHttpMessage> {
	
	private static final Logger logger = LogManager.getLogger(ApiRequestParser.class);
	
	// Http 요청을 처리하려면 HttpRequest 객체를 멤버 변수로 등록
	private HttpRequest request;
	// API 요청에 따라서 업무 처리 클래스를 호출하고 그 결과를 저장할 JsonObject 객체를 멤버 변수로 등록
	private JsonObject apiResult;
	
	private static HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
	
	// 사용자가 전송한 HTTP 요청의 본문을 추출할 디코더를 멤버 변수로 등록
	private HttpPostRequestDecoder decoder;
	
	// 사용자가 전송한 HTTP 요청의 파라미터를 업무 처리 클래스로 전달하려면 맵 객체를 멤버 변수로 등록
	private Map<String, String> reqData = new HashMap<>();
	
	// 클라이언트가 전송한 HTTP 헤더 중에서 사용할 헤더의 이름 목록을 새 객체에 저장하고
	// HTTP 요청 데이터의 헤더 정보를 추출할 때 이 멤버 변수에 포함된 필드만 사용
	private static final Set<String> usingHeader = new HashSet<>();
	static {
		usingHeader.add("token");
		usingHeader.add("email");
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpMessage msg) throws Exception {
		// Request header 처리
		// HttpRequestDecoder는 HTTP 프로토콜의 데이터를 HttpRequest, HttpContent, LastHttpContent의 순서로 디코딩하여 FullHttpMessage 객체로 만들고 인바운드 이벤트를 발생시킴
		// FullHttpMessage 인터페이스는  HttpRequest, HttpMessage, HttpContent의 최상위 인터페이스임
		if (msg instanceof HttpRequest) {
			this.request = (HttpRequest) msg;
			
			// 지정된 메시지에 'Expect : 100-continue' 헤더가 있는 경우에만 true를 반환
			// 서버가 100 Continue 값이 담긴 Expect 헤더가 포함된 요청을 받는다면, 100 Continue 응답 혹은 에러 코드로 답해야 함
			if (HttpHeaders.is100ContinueExpected(request)) {
				send100Continue(ctx);
			}
			
			// HTTP 요청의 헤더 정보 추출
			HttpHeaders headers = request.headers();
			if (!headers.isEmpty()) {
				for (Map.Entry<String, String> h : headers) {
					String key = h.getKey();
					if (usingHeader.contains(key)) {
						reqData.put(key, h.getValue());
					}
				}
			}
			
			reqData.put("REQUEST_URI", request.getUri());
			reqData.put("REQUEST_METHOD", request.getMethod().name());
		}
		
		// Request content 처리
		// HttpRequestDecoder는 HTTP 프로토콜의 데이터를 HttpRequest, HttpContent, LastHttpContent의 순서로 디코딩하여 FullHttpMessage 객체로 만들고 인바운드 이벤트를 발생시킴
		// FullHttpMessage 인터페이스는  HttpRequest, HttpMessage, HttpContent의 최상위 인터페이스임
		if (msg instanceof HttpContent) {
//			HttpContent httpContent = (HttpContent) msg;
//			
//			// HTTP 본문 데이터를 추출함
//			ByteBuf content = httpContent.content();
			
			// HttpContent의 상위 인터페이스인 LastHttpContent는 모든 HTTP 메시지가 디코딩되었고 HTTP 프로토콜의 마지막 데이터임을 알리는 인터페이스
			if (msg instanceof LastHttpContent) {
				logger.debug("LastHttpContent message received!!" + request.getUri());
				
				LastHttpContent trailer = (LastHttpContent) msg;
				
				// HTTP 본문에서 HTTP Post 데이터를 추출함
				readPostData();
				
				// HTTP 요청에 맞는 API 서비스 클래스를 생성함
				ApiRequest service = ServiceDispatcher.dispatch(reqData);
				
				try {
					// ServiceDispatcher 클래스의 dispatch 메서드로부터 생성된 API 서비스 클래스를 실행함
					service.executeService();
					
					// API 서비스 클래스의 수행 결과를 apiResult 멤버 변수에 할당함
					apiResult = service.getApiResult();
				}
				finally {
					reqData.clear();
				}
				
				// apiResult 멤버 변수에 저장된 API 처리 결과를 클라이언트 채널의 송신 버퍼에 기록함
				if (!writeResponse(trailer, ctx)) {
					ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
				}
				reset();
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		logger.info("요청 처리 완료");
		// 채널 버퍼의 내용을 클라이언트로부터 전송함
		ctx.flush();
	}
	
	private void reset() {
        request = null;
    }
	
	/**
	 * HTTP 본문에서 HTTP Post 데이터를 추출함
	 */
	private void readPostData() {
		try {
			// HTTPRequest 객체에 포함된 HTTP 본문 중에서 POST 메서드로 수신된 데이터를 추출하기 위한 디코더 생성
			decoder = new HttpPostRequestDecoder(factory, request);
			for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
				if (HttpDataType.Attribute == data.getHttpDataType()) {
					try {
						Attribute attribute = (Attribute) data;
						reqData.put(attribute.getName(), attribute.getValue());
					}
					catch(IOException e) {
						logger.error("BODY Attribute : " + data.getHttpDataType().name(), e);
						return;
					}
				}
				else {
					logger.info("Body data : " + data.getHttpDataType().name() + ": " + data);
				}
			}
		}
		catch (ErrorDataDecoderException e) {
			logger.error(e);
		}
		finally {
			if (decoder != null) {
				// exception..으로 주석 처리
//				decoder.destroy();
			}
		}
	}
	
	private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
		boolean keepAlive = HttpHeaders.isKeepAlive(request);
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
				currentObj.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
						Unpooled.copiedBuffer(apiResult.toString(), CharsetUtil.UTF_8));
		
		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		
		if (keepAlive) {
			response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
		}
		
		ctx.write(response);
		
		return keepAlive;
	}
	
	private static void send100Continue(ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
		ctx.write(response);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error(cause);
		ctx.close();
	}
	
}
