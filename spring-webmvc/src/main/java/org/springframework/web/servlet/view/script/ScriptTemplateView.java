package org.springframework.web.servlet.view.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scripting.support.StandardScriptEvalException;
import org.springframework.scripting.support.StandardScriptUtils;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * 一个{@link AbstractUrlBasedView}子类, 用于运行基于JSR-223脚本引擎的任何模板库.
 *
 * <p>如果未设置, 则通过在Web应用程序上下文中查找单个{@link ScriptTemplateConfig} bean,
 * 并使用它以获取配置的属性来自动检测每个属性.
 *
 * <p>Nashorn JavaScript引擎需要Java 8+, 并且可能需要将{@code sharedEngine}属性设置为{@code false}才能正常运行.
 * See {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} for more details.
 */
public class ScriptTemplateView extends AbstractUrlBasedView {

	public static final String DEFAULT_CONTENT_TYPE = "text/html";

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final String DEFAULT_RESOURCE_LOADER_PATH = "classpath:";


	private static final ThreadLocal<Map<Object, ScriptEngine>> enginesHolder =
			new NamedThreadLocal<Map<Object, ScriptEngine>>("ScriptTemplateView engines");


	private ScriptEngine engine;

	private String engineName;

	private Boolean sharedEngine;

	private String[] scripts;

	private String renderObject;

	private String renderFunction;

	private Charset charset;

	private String[] resourceLoaderPaths;

	private ResourceLoader resourceLoader;

	private volatile ScriptEngineManager scriptEngineManager;


	public ScriptTemplateView() {
		setContentType(null);
	}

	public ScriptTemplateView(String url) {
		super(url);
		setContentType(null);
	}


	/**
	 * See {@link ScriptTemplateConfigurer#setEngine(ScriptEngine)} documentation.
	 */
	public void setEngine(ScriptEngine engine) {
		Assert.isInstanceOf(Invocable.class, engine, "ScriptEngine must implement Invocable");
		this.engine = engine;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setEngineName(String)} documentation.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setSharedEngine(Boolean)} documentation.
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setScripts(String...)} documentation.
	 */
	public void setScripts(String... scripts) {
		this.scripts = scripts;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderObject(String)} documentation.
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setRenderFunction(String)} documentation.
	 */
	public void setRenderFunction(String functionName) {
		this.renderFunction = functionName;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setContentType(String)}} documentation.
	 */
	@Override
	public void setContentType(String contentType) {
		super.setContentType(contentType);
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setCharset(Charset)} documentation.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	/**
	 * See {@link ScriptTemplateConfigurer#setResourceLoaderPath(String)} documentation.
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		String[] paths = StringUtils.commaDelimitedListToStringArray(resourceLoaderPath);
		this.resourceLoaderPaths = new String[paths.length + 1];
		this.resourceLoaderPaths[0] = "";
		for (int i = 0; i < paths.length; i++) {
			String path = paths[i];
			if (!path.endsWith("/") && !path.endsWith(":")) {
				path = path + "/";
			}
			this.resourceLoaderPaths[i + 1] = path;
		}
	}


	@Override
	protected void initApplicationContext(ApplicationContext context) {
		super.initApplicationContext(context);

		ScriptTemplateConfig viewConfig = autodetectViewConfig();
		if (this.engine == null && viewConfig.getEngine() != null) {
			setEngine(viewConfig.getEngine());
		}
		if (this.engineName == null && viewConfig.getEngineName() != null) {
			this.engineName = viewConfig.getEngineName();
		}
		if (this.scripts == null && viewConfig.getScripts() != null) {
			this.scripts = viewConfig.getScripts();
		}
		if (this.renderObject == null && viewConfig.getRenderObject() != null) {
			this.renderObject = viewConfig.getRenderObject();
		}
		if (this.renderFunction == null && viewConfig.getRenderFunction() != null) {
			this.renderFunction = viewConfig.getRenderFunction();
		}
		if (this.getContentType() == null) {
			setContentType(viewConfig.getContentType() != null ? viewConfig.getContentType() : DEFAULT_CONTENT_TYPE);
		}
		if (this.charset == null) {
			this.charset = (viewConfig.getCharset() != null ? viewConfig.getCharset() : DEFAULT_CHARSET);
		}
		if (this.resourceLoaderPaths == null) {
			String resourceLoaderPath = viewConfig.getResourceLoaderPath();
			setResourceLoaderPath(resourceLoaderPath == null ? DEFAULT_RESOURCE_LOADER_PATH : resourceLoaderPath);
		}
		if (this.resourceLoader == null) {
			this.resourceLoader = getApplicationContext();
		}
		if (this.sharedEngine == null && viewConfig.isSharedEngine() != null) {
			this.sharedEngine = viewConfig.isSharedEngine();
		}

		Assert.isTrue(!(this.engine != null && this.engineName != null),
				"You should define either 'engine' or 'engineName', not both.");
		Assert.isTrue(!(this.engine == null && this.engineName == null),
				"No script engine found, please specify either 'engine' or 'engineName'.");

		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Assert.isTrue(this.engineName != null,
					"When 'sharedEngine' is set to false, you should specify the " +
					"script engine using the 'engineName' property, not the 'engine' one.");
		}
		else if (this.engine != null) {
			loadScripts(this.engine);
		}
		else {
			setEngine(createEngineFromName());
		}

		Assert.isTrue(this.renderFunction != null, "The 'renderFunction' property must be defined.");
	}

	protected ScriptEngine getEngine() {
		if (Boolean.FALSE.equals(this.sharedEngine)) {
			Map<Object, ScriptEngine> engines = enginesHolder.get();
			if (engines == null) {
				engines = new HashMap<Object, ScriptEngine>(4);
				enginesHolder.set(engines);
			}
			Object engineKey = (!ObjectUtils.isEmpty(this.scripts) ?
					new EngineKey(this.engineName, this.scripts) : this.engineName);
			ScriptEngine engine = engines.get(engineKey);
			if (engine == null) {
				engine = createEngineFromName();
				engines.put(engineKey, engine);
			}
			return engine;
		}
		else {
			// Simply return the configured ScriptEngine...
			return this.engine;
		}
	}

	protected ScriptEngine createEngineFromName() {
		if (this.scriptEngineManager == null) {
			this.scriptEngineManager = new ScriptEngineManager(getApplicationContext().getClassLoader());
		}

		ScriptEngine engine = StandardScriptUtils.retrieveEngineByName(this.scriptEngineManager, this.engineName);
		loadScripts(engine);
		return engine;
	}

	protected void loadScripts(ScriptEngine engine) {
		if (!ObjectUtils.isEmpty(this.scripts)) {
			for (String script : this.scripts) {
				Resource resource = getResource(script);
				if (resource == null) {
					throw new IllegalStateException("Script resource [" + script + "] not found");
				}
				try {
					engine.eval(new InputStreamReader(resource.getInputStream()));
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Failed to evaluate script [" + script + "]", ex);
				}
			}
		}
	}

	protected Resource getResource(String location) {
		for (String path : this.resourceLoaderPaths) {
			Resource resource = this.resourceLoader.getResource(path + location);
			if (resource.exists()) {
				return resource;
			}
		}
		return null;
	}

	protected ScriptTemplateConfig autodetectViewConfig() throws BeansException {
		try {
			return BeanFactoryUtils.beanOfTypeIncludingAncestors(
					getApplicationContext(), ScriptTemplateConfig.class, true, false);
		}
		catch (NoSuchBeanDefinitionException ex) {
			throw new ApplicationContextException("Expected a single ScriptTemplateConfig bean in the current " +
					"Servlet web application context or the parent root context: ScriptTemplateConfigurer is " +
					"the usual implementation. This bean may have any name.", ex);
		}
	}


	@Override
	public boolean checkResource(Locale locale) throws Exception {
		return (getResource(getUrl()) != null);
	}

	@Override
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		super.prepareResponse(request, response);

		setResponseContentType(request, response);
		response.setCharacterEncoding(this.charset.name());
	}

	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		try {
			ScriptEngine engine = getEngine();
			Invocable invocable = (Invocable) engine;
			String url = getUrl();
			String template = getTemplate(url);

			Object html;
			if (this.renderObject != null) {
				Object thiz = engine.eval(this.renderObject);
				html = invocable.invokeMethod(thiz, this.renderFunction, template, model, url);
			}
			else {
				html = invocable.invokeFunction(this.renderFunction, template, model, url);
			}

			response.getWriter().write(String.valueOf(html));
		}
		catch (ScriptException ex) {
			throw new ServletException("Failed to render script template", new StandardScriptEvalException(ex));
		}
	}

	protected String getTemplate(String path) throws IOException {
		Resource resource = getResource(path);
		if (resource == null) {
			throw new IllegalStateException("Template resource [" + path + "] not found");
		}
		InputStreamReader reader = new InputStreamReader(resource.getInputStream(), this.charset);
		return FileCopyUtils.copyToString(reader);
	}


	/**
	 * {@code enginesHolder ThreadLocal}的键类.
	 * 仅在指定了脚本时使用; 否则, {@code engineName String}将直接用作缓存键.
	 */
	private static class EngineKey {

		private final String engineName;

		private final String[] scripts;

		public EngineKey(String engineName, String[] scripts) {
			this.engineName = engineName;
			this.scripts = scripts;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof EngineKey)) {
				return false;
			}
			EngineKey otherKey = (EngineKey) other;
			return (this.engineName.equals(otherKey.engineName) && Arrays.equals(this.scripts, otherKey.scripts));
		}

		@Override
		public int hashCode() {
			return (this.engineName.hashCode() * 29 + Arrays.hashCode(this.scripts));
		}
	}

}
