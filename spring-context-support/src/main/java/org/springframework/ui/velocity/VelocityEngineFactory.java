package org.springframework.ui.velocity;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.CommonsLogLogChute;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 配置VelocityEngine的工厂.
 * 可以单独使用, 但通常可以使用{@link VelocityEngineFactoryBean}来准备VelocityEngine作为bean引用,
 * 或者将{@link org.springframework.web.servlet.view.velocity.VelocityConfigurer}用于Web视图.
 *
 * <p>可选的"configLocation"属性设置当前应用程序中Velocity属性文件的位置.
 * Velocity属性可以通过"velocityProperties"覆盖, 甚至可以在本地完全指定, 从而避免使用外部属性文件.
 *
 * <p>"resourceLoaderPath"属性可用于通过Spring的资源抽象指定Velocity资源加载器路径, 可能与Spring应用程序上下文有关.
 *
 * <p>如果"overrideLogging"为 true (默认), 则VelocityEngine将配置为通过Commons Logging进行记录,
 * 即使用{@link CommonsLogLogChute}作为日志系统.
 *
 * <p>使用此类的最简单方法是指定{@link #setResourceLoaderPath(String) "resourceLoaderPath"};
 * VelocityEngine通常不需要任何进一步的配置.
 *
 * @deprecated as of Spring 4.3, in favor of FreeMarker
 */
@Deprecated
public class VelocityEngineFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private Resource configLocation;

	private final Map<String, Object> velocityProperties = new HashMap<String, Object>();

	private String resourceLoaderPath;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;

	private boolean overrideLogging = true;


	/**
	 * 设置Velocity配置文件的位置.
	 * 或者, 可以在本地指定所有属性.
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * 设置Velocity属性, 例如"file.resource.loader.path".
	 * 可用于覆盖Velocity配置文件中的值, 或在本地指定所有必需的属性.
	 * <p>请注意, Velocity资源加载器路径也可以通过"resourceLoaderPath"属性, 设置为任何Spring资源位置.
	 * 在使用非基于文件的资源加载器时, 只需在此处设置.
	 */
	public void setVelocityProperties(Properties velocityProperties) {
		CollectionUtils.mergePropertiesIntoMap(velocityProperties, this.velocityProperties);
	}

	/**
	 * 设置Velocity属性, 以允许非字符串值, 如"ds.resource.loader.instance".
	 */
	public void setVelocityPropertiesMap(Map<String, Object> velocityPropertiesMap) {
		if (velocityPropertiesMap != null) {
			this.velocityProperties.putAll(velocityPropertiesMap);
		}
	}

	/**
	 * 通过Spring资源位置设置Velocity资源加载器路径.
	 * 接受Velocity以逗号分隔的路径样式的多个位置.
	 * <p>通过String填充时, 支持标准URL "file:"和伪URL"classpath:", 如ResourceLoader所理解的.
	 * 在ApplicationContext中运行时允许相对路径.
	 * <p>将为名称为"file"的默认Velocity资源加载器定义路径.
	 * 如果指定的资源无法解析为{@code java.io.File}, 则会在名称为"spring"的情况下使用通用的SpringResourceLoader, 而无需修改检测.
	 * <p>请注意, 在任何情况下都将启用资源缓存. 使用文件资源加载器, 将在访问时检查上次修改的时间戳以检测更改.
	 * 使用SpringResourceLoader, 资源将永久缓存(例如, 对于类路径资源).
	 * <p>要指定文件的修改检查间隔, 请使用Velocity的标准"file.resource.loader.modificationCheckInterval"属性.
	 * 默认情况下, 每次访问都会检查文件时间戳 (这种速度非常快).
	 * 当然, 这只适用于从文件系统加载资源时.
	 * <p>要强制使用SpringResourceLoader, i.e. 在任何情况下都不将路径解析为文件系统资源, 关闭"preferFileSystemAccess"标志.
	 * 有关详细信息, 请参阅后者的javadoc.
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	/**
	 * 设置用于加载Velocity模板文件的Spring ResourceLoader.
	 * 默认是DefaultResourceLoader. 如果在上下文中运行, 将被ApplicationContext覆盖.
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 返回用于加载Velocity模板文件的Spring ResourceLoader.
	 */
	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置是否更喜欢文件系统访问以进行模板加载.
	 * 文件系统访问可以热检测模板更改.
	 * <p>如果启用此选项, VelocityEngineFactory将尝试将指定的"resourceLoaderPath"解析为文件系统资源
	 * (这也适用于扩展的类路径资源和ServletContext资源).
	 * <p>默认"true".
	 * 将其关闭以始终通过SpringResourceLoader加载 (i.e. 作为流, 无需模板更改的热检测),
	 * 如果某些模板位于扩展类目录中而其他模板位于jar文件中, 则可能需要将其关闭.
	 */
	public void setPreferFileSystemAccess(boolean preferFileSystemAccess) {
		this.preferFileSystemAccess = preferFileSystemAccess;
	}

	/**
	 * 返回是否更喜欢文件系统访问以进行模板加载.
	 */
	protected boolean isPreferFileSystemAccess() {
		return this.preferFileSystemAccess;
	}

	/**
	 * 设置Velocity是否应通过Commons Logging记录, i.e. Velocity的日志系统是否应设置为{@link CommonsLogLogChute}.
	 * 默认"true".
	 */
	public void setOverrideLogging(boolean overrideLogging) {
		this.overrideLogging = overrideLogging;
	}


	/**
	 * 准备VelocityEngine实例并将其返回.
	 * 
	 * @return VelocityEngine实例
	 * @throws IOException 如果找不到配置文件
	 * @throws VelocityException Velocity初始化失败
	 */
	public VelocityEngine createVelocityEngine() throws IOException, VelocityException {
		VelocityEngine velocityEngine = newVelocityEngine();
		Map<String, Object> props = new HashMap<String, Object>();

		// Load config file if set.
		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading Velocity config from [" + this.configLocation + "]");
			}
			CollectionUtils.mergePropertiesIntoMap(PropertiesLoaderUtils.loadProperties(this.configLocation), props);
		}

		// Merge local properties if set.
		if (!this.velocityProperties.isEmpty()) {
			props.putAll(this.velocityProperties);
		}

		// Set a resource loader path, if required.
		if (this.resourceLoaderPath != null) {
			initVelocityResourceLoader(velocityEngine, this.resourceLoaderPath);
		}

		// Log via Commons Logging?
		if (this.overrideLogging) {
			velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new CommonsLogLogChute());
		}

		// Apply properties to VelocityEngine.
		for (Map.Entry<String, Object> entry : props.entrySet()) {
			velocityEngine.setProperty(entry.getKey(), entry.getValue());
		}

		postProcessVelocityEngine(velocityEngine);

		// Perform actual initialization.
		velocityEngine.init();

		return velocityEngine;
	}

	/**
	 * 返回新的VelocityEngine. 子类可以覆盖它以进行自定义初始化, 或者使用模拟对象进行测试.
	 * <p>由{@code createVelocityEngine()}调用.
	 * 
	 * @return VelocityEngine实例
	 * @throws IOException 如果找不到配置文件
	 * @throws VelocityException Velocity初始化失败
	 */
	protected VelocityEngine newVelocityEngine() throws IOException, VelocityException {
		return new VelocityEngine();
	}

	/**
	 * 为给定的VelocityEngine初始化Velocity资源加载器:
	 * 标准的Velocity FileResourceLoader或SpringResourceLoader.
	 * <p>由{@code createVelocityEngine()}调用.
	 * 
	 * @param velocityEngine 要配置的VelocityEngine
	 * @param resourceLoaderPath 从中加载Velocity资源的路径
	 */
	protected void initVelocityResourceLoader(VelocityEngine velocityEngine, String resourceLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// 尝试通过文件系统加载, 回退到SpringResourceLoader (如果可能的话, 用于模板更改的热检测).
			try {
				StringBuilder resolvedPath = new StringBuilder();
				String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
				for (int i = 0; i < paths.length; i++) {
					String path = paths[i];
					Resource resource = getResourceLoader().getResource(path);
					File file = resource.getFile();  // will fail if not resolvable in the file system
					if (logger.isDebugEnabled()) {
						logger.debug("Resource loader path [" + path + "] resolved to file [" + file.getAbsolutePath() + "]");
					}
					resolvedPath.append(file.getAbsolutePath());
					if (i < paths.length - 1) {
						resolvedPath.append(',');
					}
				}
				velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
				velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
				velocityEngine.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, resolvedPath.toString());
			}
			catch (IOException ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve resource loader path [" + resourceLoaderPath +
							"] to [java.io.File]: using SpringResourceLoader", ex);
				}
				initSpringResourceLoader(velocityEngine, resourceLoaderPath);
			}
		}
		else {
			// 始终通过SpringResourceLoader加载 (没有模板更改的热检测).
			if (logger.isDebugEnabled()) {
				logger.debug("File system access not preferred: using SpringResourceLoader");
			}
			initSpringResourceLoader(velocityEngine, resourceLoaderPath);
		}
	}

	/**
	 * 为给定的VelocityEngine初始化SpringResourceLoader.
	 * <p>由{@code initVelocityResourceLoader}调用.
	 * 
	 * @param velocityEngine 要配置的VelocityEngine
	 * @param resourceLoaderPath 从中加载Velocity资源的路径
	 */
	protected void initSpringResourceLoader(VelocityEngine velocityEngine, String resourceLoaderPath) {
		velocityEngine.setProperty(
				RuntimeConstants.RESOURCE_LOADER, SpringResourceLoader.NAME);
		velocityEngine.setProperty(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_CLASS, SpringResourceLoader.class.getName());
		velocityEngine.setProperty(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_CACHE, "true");
		velocityEngine.setApplicationAttribute(
				SpringResourceLoader.SPRING_RESOURCE_LOADER, getResourceLoader());
		velocityEngine.setApplicationAttribute(
				SpringResourceLoader.SPRING_RESOURCE_LOADER_PATH, resourceLoaderPath);
	}

	/**
	 * 由要在此FactoryBean执行其默认配置之后(但在VelocityEngine.init之前), 执行VelocityEngine的自定义后处理的子类实现.
	 * <p>由{@code createVelocityEngine()}调用.
	 * 
	 * @param velocityEngine 当前VelocityEngine
	 * 
	 * @throws IOException 如果找不到配置文件
	 * @throws VelocityException Velocity初始化失败
	 */
	protected void postProcessVelocityEngine(VelocityEngine velocityEngine)
			throws IOException, VelocityException {
	}

}
