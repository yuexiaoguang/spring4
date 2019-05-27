package org.springframework.web.servlet.view.script;

import java.nio.charset.Charset;
import javax.script.ScriptEngine;

/**
 * Interface to be implemented by objects that configure and manage a
 * JSR-223 {@link ScriptEngine} for automatic lookup in a web environment.
 * Detected and used by {@link ScriptTemplateView}.
 */
public interface ScriptTemplateConfig {

	/**
	 * Return the {@link ScriptEngine} to use by the views.
	 */
	ScriptEngine getEngine();

	/**
	 * Return the engine name that will be used to instantiate the {@link ScriptEngine}.
	 */
	String getEngineName();

	/**
	 * Return whether to use a shared engine for all threads or whether to create
	 * thread-local engine instances for each thread.
	 */
	Boolean isSharedEngine();

	/**
	 * Return the scripts to be loaded by the script engine (library or user provided).
	 */
	String[] getScripts();

	/**
	 * Return the object where the render function belongs (optional).
	 */
	String getRenderObject();

	/**
	 * Return the render function name (mandatory).
	 */
	String getRenderFunction();

	/**
	 * Return the content type to use for the response.
	 * @since 4.2.1
	 */
	String getContentType();

	/**
	 * Return the charset used to read script and template files.
	 */
	Charset getCharset();

	/**
	 * Return the resource loader path(s) via a Spring resource location.
	 */
	String getResourceLoaderPath();

}
