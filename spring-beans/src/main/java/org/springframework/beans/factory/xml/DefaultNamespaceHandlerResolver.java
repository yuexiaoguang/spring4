package org.springframework.beans.factory.xml;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link NamespaceHandlerResolver}接口的默认实现.
 * 根据映射文件中包含的映射, 将命名空间URI解析为实现类.
 *
 * <p>默认情况下, 此实现在 {@code META-INF/spring.handlers}中查找映射文件,
 * 但是这可以使用 {@link #DefaultNamespaceHandlerResolver(ClassLoader, String)}构造函数来更改.
 */
public class DefaultNamespaceHandlerResolver implements NamespaceHandlerResolver {

	/**
	 * 要查找映射文件的位置. 可以存在于多个JAR文件中.
	 */
	public static final String DEFAULT_HANDLER_MAPPINGS_LOCATION = "META-INF/spring.handlers";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 用于NamespaceHandler类的ClassLoader */
	private final ClassLoader classLoader;

	/** Resource location to search for */
	private final String handlerMappingsLocation;

	/** 存储从命名空间URI到NamespaceHandler类名称/实例的映射 */
	private volatile Map<String, Object> handlerMappings;


	/**
	 * 使用默认映射文件位置创建新的 {@code DefaultNamespaceHandlerResolver}.
	 * <p>此构造函数将导致线程上下文ClassLoader用于加载资源.
	 */
	public DefaultNamespaceHandlerResolver() {
		this(null, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * 使用默认映射文件位置创建新的 {@code DefaultNamespaceHandlerResolver}.
	 * 
	 * @param classLoader 用于加载映射资源的{@link ClassLoader}实例
	 * (可能是{@code null}, 在这种情况下, 将使用线程上下文ClassLoader)
	 */
	public DefaultNamespaceHandlerResolver(ClassLoader classLoader) {
		this(classLoader, DEFAULT_HANDLER_MAPPINGS_LOCATION);
	}

	/**
	 * @param classLoader 用于加载映射资源的{@link ClassLoader}实例
	 * (可能是{@code null}, 在这种情况下, 将使用线程上下文ClassLoader)
	 * @param handlerMappingsLocation 映射文件位置
	 */
	public DefaultNamespaceHandlerResolver(ClassLoader classLoader, String handlerMappingsLocation) {
		Assert.notNull(handlerMappingsLocation, "Handler mappings location must not be null");
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
		this.handlerMappingsLocation = handlerMappingsLocation;
	}


	/**
	 * 从配置的映射中, 定位提供的命名空间URI的{@link NamespaceHandler}.
	 * 
	 * @param namespaceUri 相关的命名空间URI
	 * 
	 * @return 定位的{@link NamespaceHandler}, 或{@code null}
	 */
	@Override
	public NamespaceHandler resolve(String namespaceUri) {
		Map<String, Object> handlerMappings = getHandlerMappings();
		Object handlerOrClassName = handlerMappings.get(namespaceUri);
		if (handlerOrClassName == null) {
			return null;
		}
		else if (handlerOrClassName instanceof NamespaceHandler) {
			return (NamespaceHandler) handlerOrClassName;
		}
		else {
			String className = (String) handlerOrClassName;
			try {
				Class<?> handlerClass = ClassUtils.forName(className, this.classLoader);
				if (!NamespaceHandler.class.isAssignableFrom(handlerClass)) {
					throw new FatalBeanException("Class [" + className + "] for namespace [" + namespaceUri +
							"] does not implement the [" + NamespaceHandler.class.getName() + "] interface");
				}
				NamespaceHandler namespaceHandler = (NamespaceHandler) BeanUtils.instantiateClass(handlerClass);
				namespaceHandler.init();
				handlerMappings.put(namespaceUri, namespaceHandler);
				return namespaceHandler;
			}
			catch (ClassNotFoundException ex) {
				throw new FatalBeanException("NamespaceHandler class [" + className + "] for namespace [" +
						namespaceUri + "] not found", ex);
			}
			catch (LinkageError err) {
				throw new FatalBeanException("Invalid NamespaceHandler class [" + className + "] for namespace [" +
						namespaceUri + "]: problem with handler class file or dependent class", err);
			}
		}
	}

	/**
	 * 延迟加载指定的NamespaceHandler映射.
	 */
	private Map<String, Object> getHandlerMappings() {
		Map<String, Object> handlerMappings = this.handlerMappings;
		if (handlerMappings == null) {
			synchronized (this) {
				handlerMappings = this.handlerMappings;
				if (handlerMappings == null) {
					if (logger.isDebugEnabled()) {
						logger.debug("Loading NamespaceHandler mappings from [" + this.handlerMappingsLocation + "]");
					}
					try {
						Properties mappings =
								PropertiesLoaderUtils.loadAllProperties(this.handlerMappingsLocation, this.classLoader);
						if (logger.isDebugEnabled()) {
							logger.debug("Loaded NamespaceHandler mappings: " + mappings);
						}
						handlerMappings = new ConcurrentHashMap<String, Object>(mappings.size());
						CollectionUtils.mergePropertiesIntoMap(mappings, handlerMappings);
						this.handlerMappings = handlerMappings;
					}
					catch (IOException ex) {
						throw new IllegalStateException(
								"Unable to load NamespaceHandler mappings from location [" + this.handlerMappingsLocation + "]", ex);
					}
				}
			}
		}
		return handlerMappings;
	}


	@Override
	public String toString() {
		return "NamespaceHandlerResolver using mappings " + getHandlerMappings();
	}

}
