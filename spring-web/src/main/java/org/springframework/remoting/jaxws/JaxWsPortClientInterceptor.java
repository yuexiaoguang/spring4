package org.springframework.remoting.jaxws;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.ProtocolException;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.SOAPFaultException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 用于访问JAX-WS服务的特定端口的{@link org.aopalliance.intercept.MethodInterceptor}.
 * 兼容JAX-WS 2.1和2.2, 包含在JDK 6 update 4+和Java 7/8中.
 *
 * <p>使用下面的{@link LocalJaxWsServiceFactory}工具, 或者显式引用现有的JAX-WS服务实例
 * (e.g. 通过{@link org.springframework.jndi.JndiObjectFactoryBean}获取).
 */
public class JaxWsPortClientInterceptor extends LocalJaxWsServiceFactory
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean {

	private Service jaxWsService;

	private String portName;

	private String username;

	private String password;

	private String endpointAddress;

	private boolean maintainSession;

	private boolean useSoapAction;

	private String soapActionUri;

	private Map<String, Object> customProperties;

	private WebServiceFeature[] portFeatures;

	private Object[] webServiceFeatures;

	private Class<?> serviceInterface;

	private boolean lookupServiceOnStartup = true;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private QName portQName;

	private Object portStub;

	private final Object preparationMonitor = new Object();


	/**
	 * 设置对现有JAX-WS服务实例的引用, 例如通过{@link org.springframework.jndi.JndiObjectFactoryBean}获取.
	 * 如果未设置, 则必须指定{@link LocalJaxWsServiceFactory}的属性.
	 */
	public void setJaxWsService(Service jaxWsService) {
		this.jaxWsService = jaxWsService;
	}

	/**
	 * 返回对现有JAX-WS服务实例的引用.
	 */
	public Service getJaxWsService() {
		return this.jaxWsService;
	}

	/**
	 * 设置端口的名称.
	 * 对应于 "wsdl:port"名称.
	 */
	public void setPortName(String portName) {
		this.portName = portName;
	}

	/**
	 * 返回端口的名称.
	 */
	public String getPortName() {
		return this.portName;
	}

	/**
	 * 设置要在存根上指定的用户名.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * 返回要在存根上指定的用户名.
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * 设置在存根上指定的密码.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * 返回在存根上指定的密码.
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * 设置要在存根上指定的端点地址.
	 */
	public void setEndpointAddress(String endpointAddress) {
		this.endpointAddress = endpointAddress;
	}

	/**
	 * 返回要在存根上指定的端点地址.
	 */
	public String getEndpointAddress() {
		return this.endpointAddress;
	}

	/**
	 * 设置在存根上指定的"session.maintain"标志.
	 */
	public void setMaintainSession(boolean maintainSession) {
		this.maintainSession = maintainSession;
	}

	/**
	 * 返回在存根上指定的"session.maintain"标志.
	 */
	public boolean isMaintainSession() {
		return this.maintainSession;
	}

	/**
	 * 设置在存根上指定的"soapaction.use"标志.
	 */
	public void setUseSoapAction(boolean useSoapAction) {
		this.useSoapAction = useSoapAction;
	}

	/**
	 * 返回在存根上指定的"soapaction.use"标志.
	 */
	public boolean isUseSoapAction() {
		return this.useSoapAction;
	}

	/**
	 * 设置要在存根上指定的SOAP操作URI.
	 */
	public void setSoapActionUri(String soapActionUri) {
		this.soapActionUri = soapActionUri;
	}

	/**
	 * 返回要在存根上指定的SOAP操作URI.
	 */
	public String getSoapActionUri() {
		return this.soapActionUri;
	}

	/**
	 * 设置要在存根上设置的自定义属性.
	 * <p>可以使用String "value" (通过PropertiesEditor解析)或XML bean定义中的"props"元素填充.
	 */
	public void setCustomProperties(Map<String, Object> customProperties) {
		this.customProperties = customProperties;
	}

	/**
	 * 允许对存根上的自定义属性进行Map访问, 并提供添加或覆盖特定条目的选项.
	 * <p>用于直接指定条目, 例如通过"customProperties[myKey]".
	 * 这对于在子bean定义中添加或覆盖条目特别有用.
	 */
	public Map<String, Object> getCustomProperties() {
		if (this.customProperties == null) {
			this.customProperties = new HashMap<String, Object>();
		}
		return this.customProperties;
	}

	/**
	 * 将自定义属性添加到此JAX-WS BindingProvider.
	 * 
	 * @param name 要公开的属性的名称
	 * @param value 要公开的属性值
	 */
	public void addCustomProperty(String name, Object value) {
		getCustomProperties().put(name, value);
	}

	/**
	 * 指定要应用于JAX-WS端口存根创建的WebServiceFeature对象 (e.g. 作为内部bean定义).
	 */
	public void setPortFeatures(WebServiceFeature... features) {
		this.portFeatures = features;
	}

	/**
	 * 为JAX-WS端口存根指定WebServiceFeature规范:
	 * 以实际的{@link javax.xml.ws.WebServiceFeature}对象, WebServiceFeature Class引用, 或WebServiceFeature类名的形式.
	 * <p>从Spring 4.0开始, 这实际上只是指定{@link #setPortFeatures "portFeatures"}的另一种方法.
	 * 不要同时指定这两个属性; 更喜欢"portFeatures".
	 * 
	 * @deprecated as of Spring 4.0, in favor of the differentiated {@link #setServiceFeatures "serviceFeatures"} and {@link #setPortFeatures "portFeatures"} properties
	 */
	@Deprecated
	public void setWebServiceFeatures(Object[] webServiceFeatures) {
		this.webServiceFeatures = webServiceFeatures;
	}

	/**
	 * 设置此工厂应为其创建代理的服务的接口.
	 */
	public void setServiceInterface(Class<?> serviceInterface) {
		if (serviceInterface != null && !serviceInterface.isInterface()) {
			throw new IllegalArgumentException("'serviceInterface' must be an interface");
		}
		this.serviceInterface = serviceInterface;
	}

	/**
	 * 返回此工厂应为其创建代理的服务的接口.
	 */
	public Class<?> getServiceInterface() {
		return this.serviceInterface;
	}

	/**
	 * 设置是否在启动时查找JAX-WS服务.
	 * <p>默认"true". 关闭此标志以允许目标服务器延迟启动.
	 * 在这种情况下, JAX-WS服务将在首次访问时延迟获取.
	 */
	public void setLookupServiceOnStartup(boolean lookupServiceOnStartup) {
		this.lookupServiceOnStartup = lookupServiceOnStartup;
	}

	/**
	 * 设置用于此拦截器的bean ClassLoader:
	 * 用于解析通过{@link #setWebServiceFeatures}指定的WebServiceFeature类名,
	 * 以及用于在{@link JaxWsPortProxyFactoryBean}子类中构建客户端代理.
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	/**
	 * 返回用于此拦截器的bean ClassLoader.
	 */
	protected ClassLoader getBeanClassLoader() {
		return this.beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.lookupServiceOnStartup) {
			prepare();
		}
	}

	/**
	 * 初始化此拦截器的JAX-WS端口.
	 */
	public void prepare() {
		Class<?> ifc = getServiceInterface();
		if (ifc == null) {
			throw new IllegalArgumentException("Property 'serviceInterface' is required");
		}
		WebService ann = ifc.getAnnotation(WebService.class);
		if (ann != null) {
			applyDefaultsFromAnnotation(ann);
		}
		Service serviceToUse = getJaxWsService();
		if (serviceToUse == null) {
			serviceToUse = createJaxWsService();
		}
		this.portQName = getQName(getPortName() != null ? getPortName() : getServiceInterface().getName());
		Object stub = getPortStub(serviceToUse, (getPortName() != null ? this.portQName : null));
		preparePortStub(stub);
		this.portStub = stub;
	}

	/**
	 * 如有必要, 可以从给定的WebService注解初始化此客户端拦截器的属性
	 * (i.e. 如果尚未设置"wsdlDocumentUrl", "namespaceUri", "serviceName"和 "portName",
	 * 但在指定的服务接口的注解级别声明了相应的值).
	 * 
	 * @param ann 在指定的服务接口上找到的WebService注解
	 */
	protected void applyDefaultsFromAnnotation(WebService ann) {
		if (getWsdlDocumentUrl() == null) {
			String wsdl = ann.wsdlLocation();
			if (StringUtils.hasText(wsdl)) {
				try {
					setWsdlDocumentUrl(new URL(wsdl));
				}
				catch (MalformedURLException ex) {
					throw new IllegalStateException(
							"Encountered invalid @Service wsdlLocation value [" + wsdl + "]", ex);
				}
			}
		}
		if (getNamespaceUri() == null) {
			String ns = ann.targetNamespace();
			if (StringUtils.hasText(ns)) {
				setNamespaceUri(ns);
			}
		}
		if (getServiceName() == null) {
			String sn = ann.serviceName();
			if (StringUtils.hasText(sn)) {
				setServiceName(sn);
			}
		}
		if (getPortName() == null) {
			String pn = ann.portName();
			if (StringUtils.hasText(pn)) {
				setPortName(pn);
			}
		}
	}

	/**
	 * 返回是否已经准备好此客户端拦截器, i.e. 已经查找了JAX-WS服务和端口.
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.portStub != null);
		}
	}

	/**
	 * 返回端口的准备好的QName.
	 */
	protected final QName getPortQName() {
		return this.portQName;
	}

	/**
	 * 从给定的JAX-WS服务获取端口存根.
	 * 
	 * @param service 从中获取端口的Service对象
	 * @param portQName 所需端口的名称
	 * 
	 * @return 从{@code Service.getPort(...)}返回的相应端口对象
	 */
	protected Object getPortStub(Service service, QName portQName) {
		if (this.portFeatures != null || this.webServiceFeatures != null) {
			WebServiceFeature[] portFeaturesToUse = this.portFeatures;
			if (portFeaturesToUse == null) {
				portFeaturesToUse = new WebServiceFeature[this.webServiceFeatures.length];
				for (int i = 0; i < this.webServiceFeatures.length; i++) {
					portFeaturesToUse[i] = convertWebServiceFeature(this.webServiceFeatures[i]);
				}
			}
			return (portQName != null ? service.getPort(portQName, getServiceInterface(), portFeaturesToUse) :
					service.getPort(getServiceInterface(), portFeaturesToUse));
		}
		else {
			return (portQName != null ? service.getPort(portQName, getServiceInterface()) :
					service.getPort(getServiceInterface()));
		}
	}

	/**
	 * 将给定的功能规范对象转换为WebServiceFeature实例
	 * 
	 * @param feature 功能规范对象, 传递到{@link #setWebServiceFeatures "webServiceFeatures"} bean属性
	 * 
	 * @return WebServiceFeature实例 (never {@code null})
	 */
	private WebServiceFeature convertWebServiceFeature(Object feature) {
		Assert.notNull(feature, "WebServiceFeature specification object must not be null");
		if (feature instanceof WebServiceFeature) {
			return (WebServiceFeature) feature;
		}
		else if (feature instanceof Class) {
			return (WebServiceFeature) BeanUtils.instantiate((Class<?>) feature);
		}
		else if (feature instanceof String) {
			try {
				Class<?> featureClass = getBeanClassLoader().loadClass((String) feature);
				return (WebServiceFeature) BeanUtils.instantiate(featureClass);
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalArgumentException("Could not load WebServiceFeature class [" + feature + "]");
			}
		}
		else {
			throw new IllegalArgumentException("Unknown WebServiceFeature specification type: " + feature.getClass());
		}
	}

	/**
	 * 准备给定的JAX-WS端口存根, 向其应用属性.
	 * 由{@link #prepare}调用.
	 * 
	 * @param stub 当前的JAX-WS端口存根
	 */
	protected void preparePortStub(Object stub) {
		Map<String, Object> stubProperties = new HashMap<String, Object>();
		String username = getUsername();
		if (username != null) {
			stubProperties.put(BindingProvider.USERNAME_PROPERTY, username);
		}
		String password = getPassword();
		if (password != null) {
			stubProperties.put(BindingProvider.PASSWORD_PROPERTY, password);
		}
		String endpointAddress = getEndpointAddress();
		if (endpointAddress != null) {
			stubProperties.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
		}
		if (isMaintainSession()) {
			stubProperties.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, Boolean.TRUE);
		}
		if (isUseSoapAction()) {
			stubProperties.put(BindingProvider.SOAPACTION_USE_PROPERTY, Boolean.TRUE);
		}
		String soapActionUri = getSoapActionUri();
		if (soapActionUri != null) {
			stubProperties.put(BindingProvider.SOAPACTION_URI_PROPERTY, soapActionUri);
		}
		stubProperties.putAll(getCustomProperties());
		if (!stubProperties.isEmpty()) {
			if (!(stub instanceof BindingProvider)) {
				throw new RemoteLookupFailureException("Port stub of class [" + stub.getClass().getName() +
						"] is not a customizable JAX-WS stub: it does not implement interface [javax.xml.ws.BindingProvider]");
			}
			((BindingProvider) stub).getRequestContext().putAll(stubProperties);
		}
	}

	/**
	 * 返回此拦截器为代理上的每个方法调用委托给的底层JAX-WS端口存根.
	 */
	protected Object getPortStub() {
		return this.portStub;
	}


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (AopUtils.isToStringMethod(invocation.getMethod())) {
			return "JAX-WS proxy for port [" + getPortName() + "] of service [" + getServiceName() + "]";
		}
		// Lazily prepare service and stub if necessary.
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		return doInvoke(invocation);
	}

	/**
	 * 基于给定的方法调用执行JAX-WS服务调用.
	 * 
	 * @param invocation AOP方法调用
	 * 
	 * @return 调用结果
	 * @throws Throwable 如果调用失败
	 */
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		try {
			return doInvoke(invocation, getPortStub());
		}
		catch (SOAPFaultException ex) {
			throw new JaxWsSoapFaultException(ex);
		}
		catch (ProtocolException ex) {
			throw new RemoteConnectFailureException(
					"Could not connect to remote service [" + getEndpointAddress() + "]", ex);
		}
		catch (WebServiceException ex) {
			throw new RemoteAccessException(
					"Could not access remote service at [" + getEndpointAddress() + "]", ex);
		}
	}

	/**
	 * 在给定的端口存根上执行JAX-WS服务调用.
	 * 
	 * @param invocation AOP方法调用
	 * @param portStub 要调用的RMI端口存根
	 * 
	 * @return 调用结果
	 * @throws Throwable 如果调用失败
	 */
	protected Object doInvoke(MethodInvocation invocation, Object portStub) throws Throwable {
		Method method = invocation.getMethod();
		try {
			return method.invoke(portStub, invocation.getArguments());
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException("Invocation of stub method failed: " + method, ex);
		}
	}

}
