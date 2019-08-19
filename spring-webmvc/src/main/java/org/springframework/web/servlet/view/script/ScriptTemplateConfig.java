package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import javax.script.ScriptEngine;

/**
 * 由配置和管理JSR-223 {@link ScriptEngine}的对象实现的接口, 以便在Web环境中自动查找.
 * 由{@link ScriptTemplateView}检测并使用.
 */
public interface ScriptTemplateConfig {

	/**
	 * 返回视图使用的{@link ScriptEngine}.
	 */
	ScriptEngine getEngine();

	/**
	 * 返回将用于实例化{@link ScriptEngine}的引擎名称.
	 */
	String getEngineName();

	/**
	 * 返回为所有线程使用共享引擎, 还是为每个线程创建线程本地引擎实例.
	 */
	Boolean isSharedEngine();

	/**
	 * 返回脚本引 (库或用户提供的)加载的脚本.
	 */
	String[] getScripts();

	/**
	 * 返回渲染函数所属的对象 (可选).
	 */
	String getRenderObject();

	/**
	 * 返回渲染函数名称 (必填).
	 */
	String getRenderFunction();

	/**
	 * 返回用于响应的内容类型.
	 */
	String getContentType();

	/**
	 * 返回用于读取脚本和模板文件的字符集.
	 */
	Charset getCharset();

	/**
	 * 通过Spring资源位置返回资源加载器路径.
	 */
	String getResourceLoaderPath();

}
