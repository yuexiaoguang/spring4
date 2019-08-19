package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import javax.script.ScriptEngine;

/**
 * Spring MVC的{@link ScriptTemplateConfig}的实现, 用于创建在Web应用程序使用的{@code ScriptEngine}.
 *
 * <pre class="code">
 *
 * // 将以下内容添加到 &#64;Configuration 类
 * &#64;Bean
 * public ScriptTemplateConfigurer mustacheConfigurer() {
 *    ScriptTemplateConfigurer configurer = new ScriptTemplateConfigurer();
 *    configurer.setEngineName("nashorn");
 *    configurer.setScripts("mustache.js");
 *    configurer.setRenderObject("Mustache");
 *    configurer.setRenderFunction("render");
 *    return configurer;
 * }
 * </pre>
 *
 * <p><b>NOTE:</b> 通过将{@link #setSharedEngine sharedEngine}属性设置为{@code false},
 * 可以使用非线程安全的脚本引擎和不是为并发而设计的模板库, 如Handlebars或在Nashorn上运行的React.
 */
public class ScriptTemplateConfigurer implements ScriptTemplateConfig {

	private ScriptEngine engine;

	private String engineName;

	private Boolean sharedEngine;

	private String[] scripts;

	private String renderObject;

	private String renderFunction;

	private String contentType;

	private Charset charset;

	private String resourceLoaderPath;


	/**
	 * 设置要由视图使用的{@link ScriptEngine}.
	 * 脚本引擎必须实现{@code Invocable}.
	 * 必须定义{@code engine} 或{@code engineName}, 而不是两者都定义.
	 * <p>当{@code sharedEngine}标志设置为{@code false}时, 不应该使用此setter指定脚本引擎,
	 * 而应使用{@link #setEngineName(String)} (因为它意味着脚本引擎的多个延迟实例化).
	 */
	public void setEngine(ScriptEngine engine) {
		this.engine = engine;
	}

	@Override
	public ScriptEngine getEngine() {
		return this.engine;
	}

	/**
	 * 设置将用于实例化{@link ScriptEngine}的引擎名称.
	 * 脚本引擎必须实现{@code Invocable}.
	 * 必须定义{@code engine}或{@code engineName}, 而不是两者都定义.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	@Override
	public String getEngineName() {
		return this.engineName;
	}

	/**
	 * 设置为{@code false}时, 使用线程本地{@link ScriptEngine}实例而不是单个共享实例.
	 * 设置为{@code false}, 对于使用非线程安全脚本引擎和不是为并发而设计的模板库, 比如在Nashorn上运行的Handlebars或React.
	 * 在这种情况下, 由于此
	 * <a href="https://bugs.openjdk.java.net/browse/JDK-8076099">this bug</a>需要Java 8u60或更高版本.
	 * <p>当此标志设置为{@code false}时, 必须使用{@link #setEngineName(String)}指定脚本引擎.
	 * 使用{@link #setEngine(ScriptEngine)}是不可能的, 因为需要延迟创建脚本引擎的多个实例 (每个线程一个).
	 * 
	 * @see <a href="http://docs.oracle.com/javase/8/docs/api/javax/script/ScriptEngineFactory.html#getParameter-java.lang.String-">THREADING ScriptEngine parameter<a/>
	 */
	public void setSharedEngine(Boolean sharedEngine) {
		this.sharedEngine = sharedEngine;
	}

	@Override
	public Boolean isSharedEngine() {
		return this.sharedEngine;
	}

	/**
	 * 设置要由脚本引擎加载的脚本 (库或用户提供的).
	 * 由于{@code resourceLoaderPath}默认值为 "classpath:", 因此可以轻松加载类路径上任何可用的脚本.
	 * <p>例如, 为了使用可用作WebJars依赖项和自定义"render.js"文件的JavaScript库, 应该调用
	 * {@code configurer.setScripts("/META-INF/resources/webjars/library/version/library.js",
	 * "com/myproject/script/render.js");}.
	 */
	public void setScripts(String... scriptNames) {
		this.scripts = scriptNames;
	}

	@Override
	public String[] getScripts() {
		return this.scripts;
	}

	/**
	 * 设置渲染函数所属的对象 (可选).
	 * 例如, 为了调用{@code Mustache.render()}, {@code renderObject}应设置为{@code "Mustache"}
	 * 以及{@code renderFunction}应设置为{@code "render"}.
	 */
	public void setRenderObject(String renderObject) {
		this.renderObject = renderObject;
	}

	@Override
	public String getRenderObject() {
		return this.renderObject;
	}

	/**
	 * 设置渲染函数名称 (必填).
	 *
	 * <p>将使用以下参数调用此函数:
	 * <ol>
	 * <li>{@code String template}: 模板内容</li>
	 * <li>{@code Map model}: 视图模型</li>
	 * <li>{@code String url}: 模板url (since 4.2.2)</li>
	 * </ol>
	 */
	public void setRenderFunction(String renderFunction) {
		this.renderFunction = renderFunction;
	}

	@Override
	public String getRenderFunction() {
		return this.renderFunction;
	}

	/**
	 * 设置要用于响应的内容类型.
	 * (默认{@code text/html}).
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回用于响应的内容类型.
	 */
	@Override
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置用于读取脚本和模板文件的字符集.
	 * (默认{@code UTF-8}).
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 通过Spring资源位置设置资源加载器路径.
	 * 接受多个位置作为逗号分隔的路径列表.
	 * Spring的{@link org.springframework.core.io.ResourceLoader}支持标准URL,
	 * 如"file:" 和 "classpath:"以及伪URL.
	 * 在ApplicationContext中运行时允许相对路径.
	 * <p>默认为"classpath:".
	 */
	public void setResourceLoaderPath(String resourceLoaderPath) {
		this.resourceLoaderPath = resourceLoaderPath;
	}

	@Override
	public String getResourceLoaderPath() {
		return this.resourceLoaderPath;
	}
}
