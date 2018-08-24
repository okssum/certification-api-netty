package ch09.apiserver;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class ApiServerMain {

	public static void main(String[] args) {
		
		AbstractApplicationContext springContext = null;
		try {
			springContext = new AnnotationConfigApplicationContext(ApiServerConfig.class);
			springContext.registerShutdownHook();
			
			ApiServer server = springContext.getBean(ApiServer.class);
			server.start();
			
//			ApiServer2 server2 = springContext.getBean(ApiServer2.class);
//			server2.start();
		}
		finally {
			springContext.close();
		}
	}

}
