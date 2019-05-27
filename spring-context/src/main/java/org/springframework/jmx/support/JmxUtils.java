package org.springframework.jmx.support;

import java.beans.PropertyDescriptor;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.List;
import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jmx.MBeanServerNotFoundException;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 支持Spring JMX的通用实用程序方法的集合.
 * 包括查找MBeanServer的便捷方法.
 */
public abstract class JmxUtils {

	/**
	 * 在使用其对应的受管资源的标识哈希码扩展现有{@link ObjectName}时, 使用的Key.
	 */
	public static final String IDENTITY_OBJECT_NAME_KEY = "identity";

	/**
	 * 用于标识MBean接口的后缀.
	 */
	private static final String MBEAN_SUFFIX = "MBean";


	private static final Log logger = LogFactory.getLog(JmxUtils.class);


	/**
	 * 尝试查找本地运行的{@code MBeanServer}.
	 * 如果找不到{@code MBeanServer}, 则会失败.
	 * 如果找到多个{@code MBeanServer}, 则记录警告, 从列表中返回第一个警告.
	 * 
	 * @return 如果找到了, 则为{@code MBeanServer}
	 * @throws MBeanServerNotFoundException 如果找不到{@code MBeanServer}
	 */
	public static MBeanServer locateMBeanServer() throws MBeanServerNotFoundException {
		return locateMBeanServer(null);
	}

	/**
	 * 尝试查找本地运行的{@code MBeanServer}.
	 * 如果找不到{@code MBeanServer}, 则会失败.
	 * 如果找到多个{@code MBeanServer}, 则记录警告, 从列表中返回第一个警告.
	 * 
	 * @param agentId 要检索的MBeanServer的代理标识符.
	 * 如果此参数为{@code null}, 则会考虑所有已注册的MBeanServers.
	 * 如果给出空字符串, 则返回平台MBeanServer.
	 * 
	 * @return 如果找到了, 则为{@code MBeanServer}
	 * @throws MBeanServerNotFoundException 如果找不到{@code MBeanServer}
	 */
	public static MBeanServer locateMBeanServer(String agentId) throws MBeanServerNotFoundException {
		MBeanServer server = null;

		// null 表示已注册的服务器, 但"" 特指平台服务器
		if (!"".equals(agentId)) {
			List<MBeanServer> servers = MBeanServerFactory.findMBeanServer(agentId);
			if (!CollectionUtils.isEmpty(servers)) {
				// 检查MBeanServer是否已注册.
				if (servers.size() > 1 && logger.isWarnEnabled()) {
					logger.warn("Found more than one MBeanServer instance" +
							(agentId != null ? " with agent id [" + agentId + "]" : "") +
							". Returning first from list.");
				}
				server = servers.get(0);
			}
		}

		if (server == null && !StringUtils.hasLength(agentId)) {
			// 尝试加载PlatformMBeanServer.
			try {
				server = ManagementFactory.getPlatformMBeanServer();
			}
			catch (SecurityException ex) {
				throw new MBeanServerNotFoundException("No specific MBeanServer found, " +
						"and not allowed to obtain the Java platform MBeanServer", ex);
			}
		}

		if (server == null) {
			throw new MBeanServerNotFoundException(
					"Unable to locate an MBeanServer instance" +
					(agentId != null ? " with agent id [" + agentId + "]" : ""));
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Found MBeanServer: " + server);
		}
		return server;
	}

	/**
	 * 将{@code MBeanParameterInfo}数组转换为与参数对应的{@code Class}实例数组.
	 * 
	 * @param paramInfo JMX参数信息
	 * 
	 * @return 参数类型
	 * @throws ClassNotFoundException 如果无法解析参数类型
	 */
	public static Class<?>[] parameterInfoToTypes(MBeanParameterInfo[] paramInfo) throws ClassNotFoundException {
		return parameterInfoToTypes(paramInfo, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 将{@code MBeanParameterInfo}数组转换为与参数对应的{@code Class}实例数组.
	 * 
	 * @param paramInfo JMX参数信息
	 * @param classLoader 用于加载参数类型的ClassLoader
	 * 
	 * @return 参数类型
	 * @throws ClassNotFoundException 如果无法解析参数类型
	 */
	public static Class<?>[] parameterInfoToTypes(MBeanParameterInfo[] paramInfo, ClassLoader classLoader)
			throws ClassNotFoundException {

		Class<?>[] types = null;
		if (paramInfo != null && paramInfo.length > 0) {
			types = new Class<?>[paramInfo.length];
			for (int x = 0; x < paramInfo.length; x++) {
				types[x] = ClassUtils.forName(paramInfo[x].getType(), classLoader);
			}
		}
		return types;
	}

	/**
	 * 创建表示方法的参数签名的{@code String[]}.
	 * 数组中的每个元素都是方法签名中相应参数的完全限定类名.
	 * 
	 * @param method 为其构建参数签名的方法
	 * 
	 * @return 作为参数类型的数组的签名
	 */
	public static String[] getMethodSignature(Method method) {
		Class<?>[] types = method.getParameterTypes();
		String[] signature = new String[types.length];
		for (int x = 0; x < types.length; x++) {
			signature[x] = types[x].getName();
		}
		return signature;
	}

	/**
	 * 返回用于给定JavaBeans属性的JMX属性名称.
	 * <p>使用严格的大小写时, 带有getter方法的JavaBean属性 {@code getFoo()} 会转换为名为{@code Foo}的属性.
	 * 禁用严格的大小写后, {@code getFoo()} 将转换为{@code foo}.
	 * 
	 * @param property JavaBeans属性描述符
	 * @param useStrictCasing 是否使用严格的大小写
	 * 
	 * @return 要使用的JMX属性名称
	 */
	public static String getAttributeName(PropertyDescriptor property, boolean useStrictCasing) {
		if (useStrictCasing) {
			return StringUtils.capitalize(property.getName());
		}
		else {
			return property.getName();
		}
	}

	/**
	 * 附加键/值对到现有 {@link ObjectName}, 其中键为静态值{@code identity}, 值为在提供的{@link ObjectName}上公开的受管资源的标识哈希码.
	 * 这可用于为特定bean或类的每个不同实例提供唯一的{@link ObjectName}.
	 * 根据{@link org.springframework.jmx.export.naming.ObjectNamingStrategy}提供的模板值,
	 * 在运行时为一组托管资源生成{@link ObjectName ObjectNames}时很有用.
	 * 
	 * @param objectName 原始的JMX ObjectName
	 * @param managedResource MBean实例
	 * 
	 * @return 添加了MBean标识的ObjectName
	 * @throws MalformedObjectNameException 如果对象名称规范无效
	 */
	public static ObjectName appendIdentityToObjectName(ObjectName objectName, Object managedResource)
			throws MalformedObjectNameException {

		Hashtable<String, String> keyProperties = objectName.getKeyPropertyList();
		keyProperties.put(IDENTITY_OBJECT_NAME_KEY, ObjectUtils.getIdentityHexString(managedResource));
		return ObjectNameManager.getInstance(objectName.getDomain(), keyProperties);
	}

	/**
	 * 返回为给定bean公开的类或接口.
	 * 这是将搜索属性和操作的类 (例如, 检查注解).
	 * <p>此实现返回CGLIB代理的超类和给定bean的类 (对于JDK代理或普通bean类).
	 * 
	 * @param managedBean bean实例 (可能是AOP代理)
	 * 
	 * @return 要暴露的bean类
	 */
	public static Class<?> getClassToExpose(Object managedBean) {
		return ClassUtils.getUserClass(managedBean);
	}

	/**
	 * 返回为给定bean类公开的类或接口.
	 * 这是将搜索属性和操作的类 (例如, 检查注解).
	 * <p>此实现返回CGLIB代理的超类和给定bean的类(对于JDK代理或普通bean类).
	 * 
	 * @param clazz bean类 (可能是AOP代理类)
	 * 
	 * @return 要暴露的bean类
	 */
	public static Class<?> getClassToExpose(Class<?> clazz) {
		return ClassUtils.getUserClass(clazz);
	}

	/**
	 * 确定给定的b​​ean类是否可以作为MBean.
	 * <p>此实现检查{@link javax.management.DynamicMBean}类以及具有相"*MBean"接口 (标准MBean)或相应"*MXBean"接口(Java 6 MXBeans)的类.
	 * 
	 * @param clazz 要分析的bean类
	 * 
	 * @return 该类是否可以作为MBean
	 */
	public static boolean isMBean(Class<?> clazz) {
		return (clazz != null &&
				(DynamicMBean.class.isAssignableFrom(clazz) ||
						(getMBeanInterface(clazz) != null || getMXBeanInterface(clazz) != null)));
	}

	/**
	 * 返回给定类的Standard MBean接口
	 * (也就是说, 一个接口, 其名称与给定类的类名匹配, 但后缀为 "MBean").
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 给定类的标准MBean接口
	 */
	public static Class<?> getMBeanInterface(Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		String mbeanInterfaceName = clazz.getName() + MBEAN_SUFFIX;
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (iface.getName().equals(mbeanInterfaceName)) {
				return iface;
			}
		}
		return getMBeanInterface(clazz.getSuperclass());
	}

	/**
	 * 返回给定类存在的Java 6 MXBean接口
	 * (也就是说, 名称以"MXBean"结尾的接口和/或带有适当的MXBean注解).
	 * 
	 * @param clazz 要检查的类
	 * 
	 * @return 是否存在给定类的MXBean接口
	 */
	public static Class<?> getMXBeanInterface(Class<?> clazz) {
		if (clazz == null || clazz.getSuperclass() == null) {
			return null;
		}
		Class<?>[] implementedInterfaces = clazz.getInterfaces();
		for (Class<?> iface : implementedInterfaces) {
			if (JMX.isMXBeanInterface(iface)) {
				return iface;
			}
		}
		return getMXBeanInterface(clazz.getSuperclass());
	}

	/**
	 * 检查MXBean支持是否可用, i.e. 是否在Java 6或更高版本上运行.
	 * 
	 * @return {@code true}可用; 否则{@code false}
	 * @deprecated as of Spring 4.0, since Java 6 is required anyway now
	 */
	@Deprecated
	public static boolean isMXBeanSupportAvailable() {
		return true;
	}
}
