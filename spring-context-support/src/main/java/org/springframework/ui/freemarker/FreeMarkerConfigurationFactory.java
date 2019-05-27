package org.springframework.ui.freemarker;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.TemplateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

/**
 * 配置FreeMarker配置的工厂.
 * 可以单独使用, 但通常使用FreeMarkerConfigurationFactoryBean将Configuration作为bean引用进行准备,
 * 或者将FreeMarkerConfigurer用于Web视图.
 *
 * <p>可选的"configLocation"属性设置当前应用程序中FreeMarker属性文件的位置.
 * FreeMarker属性可以通过"freemarkerSettings"覆盖.
 * 所有这些属性都将通过调用FreeMarker的{@code Configuration.setSettings()}方法来设置, 并受FreeMarker设置的约束.
 *
 * <p>"freemarkerVariables"属性可用于指定共享变量的Map, 该Map将通过{@code setAllSharedVariables()}方法应用于Configuration.
 * Like {@code setSettings()}, these entries are subject to FreeMarker constraints.
 *
 * <p>使用此类的最简单方法是指定"templateLoaderPath";
 * FreeMarker不需要任何进一步的配置.
 *
 * <p>Note: Spring的FreeMarker支持需要FreeMarker 2.3或更高版本.
 */
public class FreeMarkerConfigurationFactory {

	protected final Log logger = LogFactory.getLog(getClass());

	private Resource configLocation;

	private Properties freemarkerSettings;

	private Map<String, Object> freemarkerVariables;

	private String defaultEncoding;

	private final List<TemplateLoader> templateLoaders = new ArrayList<TemplateLoader>();

	private List<TemplateLoader> preTemplateLoaders;

	private List<TemplateLoader> postTemplateLoaders;

	private String[] templateLoaderPaths;

	private ResourceLoader resourceLoader = new DefaultResourceLoader();

	private boolean preferFileSystemAccess = true;


	/**
	 * 设置FreeMarker配置文件的位置.
	 * 或者, 可以在本地指定所有设置.
	 */
	public void setConfigLocation(Resource resource) {
		configLocation = resource;
	}

	/**
	 * 设置包含众所周知的FreeMarker Key的属性, 这些Key将传递给FreeMarker的{@code Configuration.setSettings}方法.
	 */
	public void setFreemarkerSettings(Properties settings) {
		this.freemarkerSettings = settings;
	}

	/**
	 * 设置包含众所周知的FreeMarker对象的Map, 这些对象将传递给FreeMarker的{@code Configuration.setAllSharedVariables()}方法.
	 */
	public void setFreemarkerVariables(Map<String, Object> variables) {
		this.freemarkerVariables = variables;
	}

	/**
	 * 设置FreeMarker配置的默认编码.
	 * 如果未指定, FreeMarker将使用平台文件编码.
	 * <p>用于模板渲染, 除非为渲染过程指定了显式编码(例如, 在Spring的FreeMarkerView上).
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	/**
	 * 设置将用于搜索模板的{@code TemplateLoader}列表.
	 * 例如, 可以在此处配置和注入一个或多个自定义加载器, 例如数据库加载器.
	 * <p>这里指定的{@link TemplateLoader TemplateLoaders}将在此工厂注册的默认模板加载器<i>之前</i>注册
	 * (例如指定的"templateLoaderPaths"的加载器或在{@link #postProcessTemplateLoaders}中注册的加载器).
	 */
	public void setPreTemplateLoaders(TemplateLoader... preTemplateLoaders) {
		this.preTemplateLoaders = Arrays.asList(preTemplateLoaders);
	}

	/**
	 * 设置将用于搜索模板的{@code TemplateLoader}.
	 * 例如, 可以配置一个或多个自定义加载器, 例如数据库加载器.
	 * <p>这里指定的{@link TemplateLoader TemplateLoaders}将在此工厂注册的默认模板加载器<i>之后</i>注册
	 * (例如指定的"templateLoaderPaths"的加载器或在{@link #postProcessTemplateLoaders}中注册的加载器).
	 */
	public void setPostTemplateLoaders(TemplateLoader... postTemplateLoaders) {
		this.postTemplateLoaders = Arrays.asList(postTemplateLoaders);
	}

	/**
	 * 通过Spring资源位置设置Freemarker模板加载器路径.
	 * 有关路径处理的详细信息, 请参阅"templateLoaderPaths"属性.
	 */
	public void setTemplateLoaderPath(String templateLoaderPath) {
		this.templateLoaderPaths = new String[] {templateLoaderPath};
	}

	/**
	 * 通过Spring资源位置设置多个Freemarker模板加载器路径.
	 * <p>通过String填充时, 支持标准URL"file:"和伪URL"classpath:", 由ResourceEditor理解.
	 * 在ApplicationContext中运行时允许相对路径.
	 * <p>将为默认的FreeMarker模板加载器定义路径.
	 * 如果无法将指定的资源解析为{@code java.io.File}, 则将使用通用的SpringTemplateLoader, 而无需修改检测.
	 * <p>要强制使用SpringTemplateLoader, i.e. 在任何情况下都不将路径解析为文件系统资源, 关闭"preferFileSystemAccess" 标志.
	 * 有关详细信息, 请参阅后者的javadoc.
	 * <p>如果指定自己的TemplateLoaders列表, 不要设置此属性, 而是使用{@code setTemplateLoaders(List templateLoaders)}
	 */
	public void setTemplateLoaderPaths(String... templateLoaderPaths) {
		this.templateLoaderPaths = templateLoaderPaths;
	}

	/**
	 * 设置用于加载FreeMarker模板文件的Spring ResourceLoader.
	 * 默认是DefaultResourceLoader. 如果在上下文中运行, 将被ApplicationContext覆盖.
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * 返回用于加载FreeMarker模板文件的Spring ResourceLoader.
	 */
	protected ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * 设置是否更喜欢文件系统访问以进行模板加载.
	 * 文件系统访问可以热检测模板更改.
	 * <p>如果启用此选项, FreeMarkerConfigurationFactory将尝试将指定的"templateLoaderPath"解析为文件系统资源
	 * (这也适用于扩展的类路径资源和ServletContext资源).
	 * <p>默认"true". 将其关闭以始终通过SpringTemplateLoader加载(i.e. 作为流, 无需模板更改的热检测),
	 * 如果某些模板驻留在扩展类目录中, 而其他模板驻留在jar文件中, 则可能需要将其关闭.
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
	 * 准备FreeMarker配置并将其返回.
	 * 
	 * @return the FreeMarker Configuration object
	 * @throws IOException 如果找不到配置文件
	 * @throws TemplateException FreeMarker初始化失败
	 */
	public Configuration createConfiguration() throws IOException, TemplateException {
		Configuration config = newConfiguration();
		Properties props = new Properties();

		// Load config file if specified.
		if (this.configLocation != null) {
			if (logger.isInfoEnabled()) {
				logger.info("Loading FreeMarker configuration from " + this.configLocation);
			}
			PropertiesLoaderUtils.fillProperties(props, this.configLocation);
		}

		// Merge local properties if specified.
		if (this.freemarkerSettings != null) {
			props.putAll(this.freemarkerSettings);
		}

		// FreeMarker只接受其setSettings和setAllSharedVariables方法中的已知键.
		if (!props.isEmpty()) {
			config.setSettings(props);
		}

		if (!CollectionUtils.isEmpty(this.freemarkerVariables)) {
			config.setAllSharedVariables(new SimpleHash(this.freemarkerVariables, config.getObjectWrapper()));
		}

		if (this.defaultEncoding != null) {
			config.setDefaultEncoding(this.defaultEncoding);
		}

		List<TemplateLoader> templateLoaders = new LinkedList<TemplateLoader>(this.templateLoaders);

		// 注册应该提前启动的模板加载器.
		if (this.preTemplateLoaders != null) {
			templateLoaders.addAll(this.preTemplateLoaders);
		}

		// 注册默认模板加载器.
		if (this.templateLoaderPaths != null) {
			for (String path : this.templateLoaderPaths) {
				templateLoaders.add(getTemplateLoaderForPath(path));
			}
		}
		postProcessTemplateLoaders(templateLoaders);

		// 注册应该后启动的模板加载器.
		if (this.postTemplateLoaders != null) {
			templateLoaders.addAll(this.postTemplateLoaders);
		}

		TemplateLoader loader = getAggregateTemplateLoader(templateLoaders);
		if (loader != null) {
			config.setTemplateLoader(loader);
		}

		postProcessConfiguration(config);
		return config;
	}

	/**
	 * 返回一个新的Configuration对象.
	 * 子类可以覆盖它以进行自定义初始化(e.g. 指定FreeMarker兼容级别, 这是FreeMarker 2.3.21中的新功能), 或者使用模拟对象进行测试.
	 * <p>由{@code createConfiguration()}调用.
	 * 
	 * @return the Configuration object
	 * @throws IOException 如果找不到配置文件
	 * @throws TemplateException FreeMarker初始化失败
	 */
	protected Configuration newConfiguration() throws IOException, TemplateException {
		return new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
	}

	/**
	 * 确定给定路径的FreeMarker TemplateLoader.
	 * <p>默认实现创建FileTemplateLoader或SpringTemplateLoader.
	 * 
	 * @param templateLoaderPath 从中加载模板的路径
	 * 
	 * @return 合适的TemplateLoader
	 */
	protected TemplateLoader getTemplateLoaderForPath(String templateLoaderPath) {
		if (isPreferFileSystemAccess()) {
			// 尝试通过文件系统加载, 回退到SpringTemplateLoader (如果可能的话, 用于模板更改的热检测).
			try {
				Resource path = getResourceLoader().getResource(templateLoaderPath);
				File file = path.getFile();  // 如果在文件系统中无法解析, 则会失败
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Template loader path [" + path + "] resolved to file path [" + file.getAbsolutePath() + "]");
				}
				return new FileTemplateLoader(file);
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Cannot resolve template loader path [" + templateLoaderPath +
							"] to [java.io.File]: using SpringTemplateLoader as fallback", ex);
				}
				return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
			}
		}
		else {
			// 始终通过SpringTemplateLoader加载(没有模板更改的热检测).
			logger.debug("File system access not preferred: using SpringTemplateLoader");
			return new SpringTemplateLoader(getResourceLoader(), templateLoaderPath);
		}
	}

	/**
	 * 由在此工厂创建其默认模板加载器之后, 注册自定义TemplateLoader实例的子类重写.
	 * <p>由{@code createConfiguration()}调用.
	 * 请注意, 指定的"postTemplateLoaders"将在此回调注册的任何加载器<i>之后</i>注册;
	 * 因此, 他们<i>不</i>包含在给定的列表中.
	 * 
	 * @param templateLoaders 当前的TemplateLoader实例列表, 由子类修改
	 */
	protected void postProcessTemplateLoaders(List<TemplateLoader> templateLoaders) {
	}

	/**
	 * 根据给定的TemplateLoader列表返回TemplateLoader.
	 * 如果已注册多个TemplateLoader, 则需要创建FreeMarker MultiTemplateLoader.
	 * 
	 * @param templateLoaders 最终的TemplateLoader实例列表
	 *
	 * @return 聚合TemplateLoader
	 */
	protected TemplateLoader getAggregateTemplateLoader(List<TemplateLoader> templateLoaders) {
		int loaderCount = templateLoaders.size();
		switch (loaderCount) {
			case 0:
				logger.info("No FreeMarker TemplateLoaders specified");
				return null;
			case 1:
				return templateLoaders.get(0);
			default:
				TemplateLoader[] loaders = templateLoaders.toArray(new TemplateLoader[loaderCount]);
				return new MultiTemplateLoader(loaders);
		}
	}

	/**
	 * 由在本工厂执行其默认初始化后, 执行Configuration对象的自定义后处理的子类重写.
	 * <p>由{@code createConfiguration()}调用.
	 * 
	 * @param config 当前的Configuration对象
	 * 
	 * @throws IOException 如果找不到配置文件
	 * @throws TemplateException FreeMarker初始化失败
	 */
	protected void postProcessConfiguration(Configuration config) throws IOException, TemplateException {
	}

}
