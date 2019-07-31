package org.springframework.remoting.jaxws;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.handler.HandlerResolver;

import org.springframework.core.io.Resource;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;

/**
 * 用于本地定义的JAX-WS {@link javax.xml.ws.Service}引用的工厂.
 * 使用下面的JAX-WS {@link javax.xml.ws.Service#create}工厂API.
 *
 * <p>用作{@link LocalJaxWsServiceFactoryBean}, {@link JaxWsPortClientInterceptor}和{@link JaxWsPortProxyFactoryBean}的基类.
 */
public class LocalJaxWsServiceFactory {

	private URL wsdlDocumentUrl;

	private String namespaceUri;

	private String serviceName;

	private WebServiceFeature[] serviceFeatures;

	private Executor executor;

	private HandlerResolver handlerResolver;


	/**
	 * 设置描述服务的WSDL文档的URL.
	 */
	public void setWsdlDocumentUrl(URL wsdlDocumentUrl) {
		this.wsdlDocumentUrl = wsdlDocumentUrl;
	}

	/**
	 * 设置WSDL文档URL, 作为{@link Resource}.
	 */
	public void setWsdlDocumentResource(Resource wsdlDocumentResource) throws IOException {
		Assert.notNull(wsdlDocumentResource, "WSDL Resource must not be null.");
		this.wsdlDocumentUrl = wsdlDocumentResource.getURL();
	}

	/**
	 * 返回描述服务的WSDL文档的URL.
	 */
	public URL getWsdlDocumentUrl() {
		return this.wsdlDocumentUrl;
	}

	/**
	 * 设置服务的命名空间URI.
	 * 对应于 WSDL "targetNamespace".
	 */
	public void setNamespaceUri(String namespaceUri) {
		this.namespaceUri = (namespaceUri != null ? namespaceUri.trim() : null);
	}

	/**
	 * 返回服务的命名空间URI.
	 */
	public String getNamespaceUri() {
		return this.namespaceUri;
	}

	/**
	 * 设置要查找的服务的名称.
	 * 对应于"wsdl:service"名称.
	 */
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	/**
	 * 返回服务的名称.
	 */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * 指定要应用于JAX-WS服务创建的WebServiceFeature对象 (e.g. 作为内部bean定义).
	 * <p>Note: 此机制需要JAX-WS 2.2或更高版本.
	 */
	public void setServiceFeatures(WebServiceFeature... serviceFeatures) {
		this.serviceFeatures = serviceFeatures;
	}

	/**
	 * 设置JDK并发执行器以用于需要回调的异步执行.
	 */
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	/**
	 * 设置用于通过此工厂创建的所有代理和调度器的JAX-WS HandlerResolver.
	 */
	public void setHandlerResolver(HandlerResolver handlerResolver) {
		this.handlerResolver = handlerResolver;
	}


	/**
	 * 根据此工厂的参数创建JAX-WS服务.
	 */
	@UsesJava7  // optional use of Service#create with WebServiceFeature[]
	public Service createJaxWsService() {
		Assert.notNull(this.serviceName, "No service name specified");
		Service service;

		if (this.serviceFeatures != null) {
			service = (this.wsdlDocumentUrl != null ?
				Service.create(this.wsdlDocumentUrl, getQName(this.serviceName), this.serviceFeatures) :
				Service.create(getQName(this.serviceName), this.serviceFeatures));
		}
		else {
			service = (this.wsdlDocumentUrl != null ?
					Service.create(this.wsdlDocumentUrl, getQName(this.serviceName)) :
					Service.create(getQName(this.serviceName)));
		}

		if (this.executor != null) {
			service.setExecutor(this.executor);
		}
		if (this.handlerResolver != null) {
			service.setHandlerResolver(this.handlerResolver);
		}

		return service;
	}

	/**
	 * 如果给定, 则返回相对于此工厂的命名空间URI的给定名称的QName.
	 */
	protected QName getQName(String name) {
		return (getNamespaceUri() != null ? new QName(getNamespaceUri(), name) : new QName(name));
	}
}
