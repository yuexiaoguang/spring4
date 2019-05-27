package org.springframework.jmx.access;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.JMException;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXServiceURL;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.CollectionFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.jmx.support.JmxUtils;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor}将调用路由到在提供的{@code MBeanServerConnection}上运行的MBean.
 * 适用于本地和远程 {@code MBeanServerConnection}s.
 *
 * <p>默认情况下, {@code MBeanClientInterceptor} 将连接到{@code MBeanServer}, 并在启动时缓存MBean元数据.
 * 应用程序启动时, 在未运行的远程{@code MBeanServer}上运行时, 这可能是不合需要的.
 * 通过将{@link #setConnectOnStartup(boolean) connectOnStartup}属性设置为"false", 可以将此过程推迟到代理的第一次调用.
 *
 * <p>此功能通常通过{@link MBeanProxyFactoryBean}使用.
 * 有关更多信息, 请参阅该类的javadoc.
 */
public class MBeanClientInterceptor
		implements MethodInterceptor, BeanClassLoaderAware, InitializingBean, DisposableBean {

	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private MBeanServerConnection server;

	private JMXServiceURL serviceUrl;

	private Map<String, ?> environment;

	private String agentId;

	private boolean connectOnStartup = true;

	private boolean refreshOnConnectFailure = false;

	private ObjectName objectName;

	private boolean useStrictCasing = true;

	private Class<?> managementInterface;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private final ConnectorDelegate connector = new ConnectorDelegate();

	private MBeanServerConnection serverToUse;

	private MBeanServerInvocationHandler invocationHandler;

	private Map<String, MBeanAttributeInfo> allowedAttributes;

	private Map<MethodCacheKey, MBeanOperationInfo> allowedOperations;

	private final Map<Method, String[]> signatureCache = new HashMap<Method, String[]>();

	private final Object preparationMonitor = new Object();


	/**
	 * 设置用于连接所有调用将路由到的MBean的{@code MBeanServerConnection}.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * 设置远程{@code MBeanServer}的服务URL.
	 */
	public void setServiceUrl(String url) throws MalformedURLException {
		this.serviceUrl = new JMXServiceURL(url);
	}

	/**
	 * 指定JMX连接器的环境.
	 */
	public void setEnvironment(Map<String, ?> environment) {
		this.environment = environment;
	}

	/**
	 * 允许访问为连接器设置的环境, 并添加或覆盖特定条目.
	 * <p>用于直接指定条目, 例如通过 "environment[myKey]".
	 * 这对于在子bean定义中添加或覆盖条目特别有用.
	 */
	public Map<String, ?> getEnvironment() {
		return this.environment;
	}

	/**
	 * 设置{@code MBeanServer}的代理ID以进行定位.
	 * <p>默认无. 如果指定, 这将导致尝试定位助理MBeanServer, 除非已设置 {@link #setServiceUrl "serviceUrl"}属性.
	 * <p>指定空字符串表示平台MBeanServer.
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * 设置代理是否应在创建时 ("true")或第一次调用时 ("false")连接到{@code MBeanServer}.
	 * 默认 "true".
	 */
	public void setConnectOnStartup(boolean connectOnStartup) {
		this.connectOnStartup = connectOnStartup;
	}

	/**
	 * 设置是否在连接失败时刷新MBeanServer连接.
	 * 默认 "false".
	 * <p>可以打开以允许热重启JMX服务器, 在IOException的情况下自动重新连接和重试.
	 */
	public void setRefreshOnConnectFailure(boolean refreshOnConnectFailure) {
		this.refreshOnConnectFailure = refreshOnConnectFailure;
	}

	/**
	 * 设置调用要路由到的MBean的{@code ObjectName}, 作为{@code ObjectName}实例或{@code String}.
	 */
	public void setObjectName(Object objectName) throws MalformedObjectNameException {
		this.objectName = ObjectNameManager.getInstance(objectName);
	}

	/**
	 * 设置是否对属性使用严格的大小写. 默认启用.
	 * <p>使用严格模式时, 带有getter的JavaBean属性 {@code getFoo()} 会转换为名为 {@code Foo}的属性.
	 * 如果禁用严格模式, {@code getFoo()} 将转换为 {@code foo}.
	 */
	public void setUseStrictCasing(boolean useStrictCasing) {
		this.useStrictCasing = useStrictCasing;
	}

	/**
	 * 设置目标MBean的管理接口, 公开MBean属性的bean属性setter和getter, 以及用于MBean操作的Java方法.
	 */
	public void setManagementInterface(Class<?> managementInterface) {
		this.managementInterface = managementInterface;
	}

	/**
	 * 返回目标MBean的管理接口, 如果没有指定, 则返回{@code null}.
	 */
	protected final Class<?> getManagementInterface() {
		return this.managementInterface;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	/**
	 * 如果打开"connectOnStartup"(默认), 则准备{@code MBeanServerConnection}.
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.server != null && this.refreshOnConnectFailure) {
			throw new IllegalArgumentException("'refreshOnConnectFailure' does not work when setting " +
					"a 'server' reference. Prefer 'serviceUrl' etc instead.");
		}
		if (this.connectOnStartup) {
			prepare();
		}
	}

	/**
	 * 确保配置了{@code MBeanServerConnection}并尝试检测本地连接, 如果未提供本地连接.
	 */
	public void prepare() {
		synchronized (this.preparationMonitor) {
			if (this.server != null) {
				this.serverToUse = this.server;
			}
			else {
				this.serverToUse = null;
				this.serverToUse = this.connector.connect(this.serviceUrl, this.environment, this.agentId);
			}
			this.invocationHandler = null;
			if (this.useStrictCasing) {
				// 使用JDK自己的MBeanServerInvocationHandler, 特别是对本机MXBean支持.
				this.invocationHandler = new MBeanServerInvocationHandler(this.serverToUse, this.objectName,
						(this.managementInterface != null && JMX.isMXBeanInterface(this.managementInterface)));
			}
			else {
				// 非严格模式只能通过自定义调用处理来实现.
				// 仅提供部分MXBean支持!
				retrieveMBeanInfo();
			}
		}
	}
	/**
	 * 将已配置的MBean的管理接口信息加载到缓存中.
	 * 当确定调用是否与受管资源的管理接口上的有效操作或属性匹配时, 代理使用此信息.
	 */
	private void retrieveMBeanInfo() throws MBeanInfoRetrievalException {
		try {
			MBeanInfo info = this.serverToUse.getMBeanInfo(this.objectName);

			MBeanAttributeInfo[] attributeInfo = info.getAttributes();
			this.allowedAttributes = new HashMap<String, MBeanAttributeInfo>(attributeInfo.length);
			for (MBeanAttributeInfo infoEle : attributeInfo) {
				this.allowedAttributes.put(infoEle.getName(), infoEle);
			}

			MBeanOperationInfo[] operationInfo = info.getOperations();
			this.allowedOperations = new HashMap<MethodCacheKey, MBeanOperationInfo>(operationInfo.length);
			for (MBeanOperationInfo infoEle : operationInfo) {
				Class<?>[] paramTypes = JmxUtils.parameterInfoToTypes(infoEle.getSignature(), this.beanClassLoader);
				this.allowedOperations.put(new MethodCacheKey(infoEle.getName(), paramTypes), infoEle);
			}
		}
		catch (ClassNotFoundException ex) {
			throw new MBeanInfoRetrievalException("Unable to locate class specified in method signature", ex);
		}
		catch (IntrospectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName + "]", ex);
		}
		catch (InstanceNotFoundException ex) {
			// if we are this far this shouldn't happen, but...
			throw new MBeanInfoRetrievalException("Unable to obtain MBean info for bean [" + this.objectName +
					"]: it is likely that this bean was unregistered during the proxy creation process",
					ex);
		}
		catch (ReflectionException ex) {
			throw new MBeanInfoRetrievalException("Unable to read MBean info for bean [ " + this.objectName + "]", ex);
		}
		catch (IOException ex) {
			throw new MBeanInfoRetrievalException("An IOException occurred when communicating with the " +
					"MBeanServer. It is likely that you are communicating with a remote MBeanServer. " +
					"Check the inner exception for exact details.", ex);
		}
	}

	/**
	 * 返回是否已准备好此客户端拦截器, i.e. 已经查找了服务器并缓存了所有元数据.
	 */
	protected boolean isPrepared() {
		synchronized (this.preparationMonitor) {
			return (this.serverToUse != null);
		}
	}


	/**
	 * 将调用路由到配置的受管资源..
	 * 
	 * @param invocation 要重新路由的{@code MethodInvocation}
	 * 
	 * @return 由于重新路由调用而返回的值
	 * @throws Throwable 传播给用户的调用错误
	 */
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		// Lazily connect to MBeanServer if necessary.
		synchronized (this.preparationMonitor) {
			if (!isPrepared()) {
				prepare();
			}
		}
		try {
			return doInvoke(invocation);
		}
		catch (MBeanConnectFailureException ex) {
			return handleConnectFailure(invocation, ex);
		}
		catch (IOException ex) {
			return handleConnectFailure(invocation, ex);
		}
	}

	/**
	 * 刷新连接并尽可能重试MBean调用.
	 * <p>如果未配置为在连接失败时刷新, 则此方法只是重新抛出原始异常.
	 * 
	 * @param invocation 失败的调用
	 * @param ex 远程调用引发的异常
	 * 
	 * @return 如果成功, 新调用的结果值
	 * @throws Throwable 如果再次失败, 新调用引发的异常
	 */
	protected Object handleConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshOnConnectFailure) {
			String msg = "Could not connect to JMX server - retrying";
			if (logger.isDebugEnabled()) {
				logger.warn(msg, ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn(msg);
			}
			prepare();
			return doInvoke(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 将调用路由到配置的受管资源.
	 * 正确地将JavaBean属性访问路由到{@code MBeanServerConnection.get/setAttribute},
	 * 并将方法调用路由到{@code MBeanServerConnection.invoke}.
	 * 
	 * @param invocation 要重新路由的{@code MethodInvocation}
	 * 
	 * @return 由于重新路由调用而返回的值
	 * @throws Throwable 传播给用户的调用错误
	 */
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		try {
			Object result = null;
			if (this.invocationHandler != null) {
				result = this.invocationHandler.invoke(invocation.getThis(), method, invocation.getArguments());
			}
			else {
				PropertyDescriptor pd = BeanUtils.findPropertyForMethod(method);
				if (pd != null) {
					result = invokeAttribute(pd, invocation);
				}
				else {
					result = invokeOperation(method, invocation.getArguments());
				}
			}
			return convertResultValueIfNecessary(result, new MethodParameter(method, -1));
		}
		catch (MBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeMBeanException ex) {
			throw ex.getTargetException();
		}
		catch (RuntimeErrorException ex) {
			throw ex.getTargetError();
		}
		catch (RuntimeOperationsException ex) {
			// 这个只由JMX 1.2 RI抛出, 而不是由JDK 1.5 JMX代码抛出.
			RuntimeException rex = ex.getTargetException();
			if (rex instanceof RuntimeMBeanException) {
				throw ((RuntimeMBeanException) rex).getTargetException();
			}
			else if (rex instanceof RuntimeErrorException) {
				throw ((RuntimeErrorException) rex).getTargetError();
			}
			else {
				throw rex;
			}
		}
		catch (OperationsException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvalidInvocationException(ex.getMessage());
			}
		}
		catch (JMException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new InvocationFailureException("JMX access failed", ex);
			}
		}
		catch (IOException ex) {
			if (ReflectionUtils.declaresException(method, ex.getClass())) {
				throw ex;
			}
			else {
				throw new MBeanConnectFailureException("I/O failure during JMX access", ex);
			}
		}
	}

	private Object invokeAttribute(PropertyDescriptor pd, MethodInvocation invocation)
			throws JMException, IOException {

		String attributeName = JmxUtils.getAttributeName(pd, this.useStrictCasing);
		MBeanAttributeInfo inf = this.allowedAttributes.get(attributeName);
		// 如果没有返回任何属性, 则它没有在管理接口中定义.
		if (inf == null) {
			throw new InvalidInvocationException(
					"Attribute '" + pd.getName() + "' is not exposed on the management interface");
		}
		if (invocation.getMethod().equals(pd.getReadMethod())) {
			if (inf.isReadable()) {
				return this.serverToUse.getAttribute(this.objectName, attributeName);
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not readable");
			}
		}
		else if (invocation.getMethod().equals(pd.getWriteMethod())) {
			if (inf.isWritable()) {
				this.serverToUse.setAttribute(this.objectName, new Attribute(attributeName, invocation.getArguments()[0]));
				return null;
			}
			else {
				throw new InvalidInvocationException("Attribute '" + attributeName + "' is not writable");
			}
		}
		else {
			throw new IllegalStateException(
					"Method [" + invocation.getMethod() + "] is neither a bean property getter nor a setter");
		}
	}

	/**
	 * 将方法调用(不是属性获取/设置) 路由到受管资源上的相应操作.
	 * 
	 * @param method 对应于对受管资源进行操作的方法.
	 * @param args 调用参数
	 * 
	 * @return 方法调用返回的值.
	 */
	private Object invokeOperation(Method method, Object[] args) throws JMException, IOException {
		MethodCacheKey key = new MethodCacheKey(method.getName(), method.getParameterTypes());
		MBeanOperationInfo info = this.allowedOperations.get(key);
		if (info == null) {
			throw new InvalidInvocationException("Operation '" + method.getName() +
					"' is not exposed on the management interface");
		}
		String[] signature = null;
		synchronized (this.signatureCache) {
			signature = this.signatureCache.get(method);
			if (signature == null) {
				signature = JmxUtils.getMethodSignature(method);
				this.signatureCache.put(method, signature);
			}
		}
		return this.serverToUse.invoke(this.objectName, method.getName(), args, signature);
	}

	/**
	 * 将给定的结果对象 (从属性访问或操作调用) 转换为指定的目标类, 以便从代理方法返回.
	 * 
	 * @param result {@code MBeanServer}返回的结果对象
	 * @param parameter 已调用的代理方法的方法参数
	 * 
	 * @return 转换的结果对象; 或传入的对象, 如果不需要转换
	 */
	protected Object convertResultValueIfNecessary(Object result, MethodParameter parameter) {
		Class<?> targetClass = parameter.getParameterType();
		try {
			if (result == null) {
				return null;
			}
			if (ClassUtils.isAssignableValue(targetClass, result)) {
				return result;
			}
			if (result instanceof CompositeData) {
				Method fromMethod = targetClass.getMethod("from", CompositeData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof CompositeData[]) {
				CompositeData[] array = (CompositeData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			else if (result instanceof TabularData) {
				Method fromMethod = targetClass.getMethod("from", TabularData.class);
				return ReflectionUtils.invokeMethod(fromMethod, null, result);
			}
			else if (result instanceof TabularData[]) {
				TabularData[] array = (TabularData[]) result;
				if (targetClass.isArray()) {
					return convertDataArrayToTargetArray(array, targetClass);
				}
				else if (Collection.class.isAssignableFrom(targetClass)) {
					Class<?> elementType =
							ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
					if (elementType != null) {
						return convertDataArrayToTargetCollection(array, targetClass, elementType);
					}
				}
			}
			throw new InvocationFailureException(
					"Incompatible result value [" + result + "] for target type [" + targetClass.getName() + "]");
		}
		catch (NoSuchMethodException ex) {
			throw new InvocationFailureException(
					"Could not obtain 'from(CompositeData)' / 'from(TabularData)' method on target type [" +
							targetClass.getName() + "] for conversion of MXBean data structure [" + result + "]");
		}
	}

	private Object convertDataArrayToTargetArray(Object[] array, Class<?> targetClass) throws NoSuchMethodException {
		Class<?> targetType = targetClass.getComponentType();
		Method fromMethod = targetType.getMethod("from", array.getClass().getComponentType());
		Object resultArray = Array.newInstance(targetType, array.length);
		for (int i = 0; i < array.length; i++) {
			Array.set(resultArray, i, ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultArray;
	}

	private Collection<?> convertDataArrayToTargetCollection(Object[] array, Class<?> collectionType, Class<?> elementType)
			throws NoSuchMethodException {

		Method fromMethod = elementType.getMethod("from", array.getClass().getComponentType());
		Collection<Object> resultColl = CollectionFactory.createCollection(collectionType, Array.getLength(array));
		for (int i = 0; i < array.length; i++) {
			resultColl.add(ReflectionUtils.invokeMethod(fromMethod, null, array[i]));
		}
		return resultColl;
	}


	@Override
	public void destroy() {
		this.connector.close();
	}


	/**
	 * 围绕方法名称及其签名的简单包装类.
	 * 用作缓存方法时的键.
	 */
	private static final class MethodCacheKey implements Comparable<MethodCacheKey> {

		private final String name;

		private final Class<?>[] parameterTypes;

		/**
		 * @param name 方法名称
		 * @param parameterTypes 方法签名中的参数
		 */
		public MethodCacheKey(String name, Class<?>[] parameterTypes) {
			this.name = name;
			this.parameterTypes = (parameterTypes != null ? parameterTypes : new Class<?>[0]);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			MethodCacheKey otherKey = (MethodCacheKey) other;
			return (this.name.equals(otherKey.name) && Arrays.equals(this.parameterTypes, otherKey.parameterTypes));
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return this.name + "(" + StringUtils.arrayToCommaDelimitedString(this.parameterTypes) + ")";
		}

		@Override
		public int compareTo(MethodCacheKey other) {
			int result = this.name.compareTo(other.name);
			if (result != 0) {
				return result;
			}
			if (this.parameterTypes.length < other.parameterTypes.length) {
				return -1;
			}
			if (this.parameterTypes.length > other.parameterTypes.length) {
				return 1;
			}
			for (int i = 0; i < this.parameterTypes.length; i++) {
				result = this.parameterTypes[i].getName().compareTo(other.parameterTypes[i].getName());
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}
}
