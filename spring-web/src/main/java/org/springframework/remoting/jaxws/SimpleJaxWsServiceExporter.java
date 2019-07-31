package org.springframework.remoting.jaxws;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceProvider;

/**
 * 用于JAX-WS服务的简单导出器, 自动检测带注解的服务bean (通过JAX-WS {@link javax.jws.WebService}注解),
 * 并使用配置的基址(默认为"http://localhost:8080/")导出它们, 使用JAX-WS提供者的内置发布支持.
 * 每个服务的完整地址将包含附加服务名称的基址 (e.g. "http://localhost:8080/OrderService").
 *
 * <p>请注意, 此导出器仅在JAX-WS运行时实际支持使用address参数发布时才起作用, i.e. 如果JAX-WS运行时附带内部HTTP服务器.
 * 这是JAX-WS运行时的情况, 它包含在Sun的JDK 6中, 但不包含在独立的JAX-WS 2.1 RI中.
 *
 * <p>要使用Sun的JDK 6 HTTP服务器显式配置JAX-WS端点, 考虑使用{@link SimpleHttpServerJaxWsServiceExporter}!
 */
public class SimpleJaxWsServiceExporter extends AbstractJaxWsServiceExporter {

	public static final String DEFAULT_BASE_ADDRESS = "http://localhost:8080/";

	private String baseAddress = DEFAULT_BASE_ADDRESS;


	/**
	 * 设置导出服务的基址.
	 * 默认"http://localhost:8080/".
	 * <p>对于每个实际发布地址, 服务名称将附加到此基址.
	 * E.g. 服务名称"OrderService" -> "http://localhost:8080/OrderService".
	 */
	public void setBaseAddress(String baseAddress) {
		this.baseAddress = baseAddress;
	}


	@Override
	protected void publishEndpoint(Endpoint endpoint, WebService annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	@Override
	protected void publishEndpoint(Endpoint endpoint, WebServiceProvider annotation) {
		endpoint.publish(calculateEndpointAddress(endpoint, annotation.serviceName()));
	}

	/**
	 * 计算给定端点的完整端点地址.
	 * 
	 * @param endpoint JAX-WS Provider Endpoint对象
	 * @param serviceName 给定服务名称
	 * 
	 * @return 完整的端点地址
	 */
	protected String calculateEndpointAddress(Endpoint endpoint, String serviceName) {
		String fullAddress = this.baseAddress + serviceName;
		if (endpoint.getClass().getName().startsWith("weblogic.")) {
			// Workaround for WebLogic 10.3
			fullAddress = fullAddress + "/";
		}
		return fullAddress;
	}

}
