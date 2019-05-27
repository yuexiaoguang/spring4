package org.springframework.scripting.support;

import java.io.IOException;
import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code javax.script} (JSR-223) 基于Spring的{@link ScriptEvaluator}策略接口的实现.
 */
public class StandardScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

	private volatile ScriptEngineManager scriptEngineManager;

	private String engineName;


	public StandardScriptEvaluator() {
	}

	/**
	 * @param classLoader 用于脚本引擎检测的类加载器
	 */
	public StandardScriptEvaluator(ClassLoader classLoader) {
		this.scriptEngineManager = new ScriptEngineManager(classLoader);
	}

	/**
	 * 为给定的JSR-223 {@link ScriptEngineManager}构建新的{@code StandardScriptEvaluator}, 以从中获取脚本引擎.
	 * 
	 * @param scriptEngineManager 要使用的ScriptEngineManager (或其子类)
	 */
	public StandardScriptEvaluator(ScriptEngineManager scriptEngineManager) {
		this.scriptEngineManager = scriptEngineManager;
	}


	/**
	 * 设置用于评估脚本的语言的名称 (e.g. "Groovy").
	 * <p>这实际上是{@link #setEngineName "engineName"}的别名,
	 * 可能 (但尚未)为JSR-223脚本引擎工厂公开的某些语言提供通用缩写.
	 */
	public void setLanguage(String language) {
		this.engineName = language;
	}

	/**
	 * 设置用于评估脚本的脚本引擎的名称 (e.g. "Groovy"), 由JSR-223脚本引擎工厂公开.
	 */
	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	/**
	 * 在底层脚本引擎管理器上设置全局范围的绑定, 由所有脚本共享, 作为脚本参数绑定的替代方法.
	 */
	public void setGlobalBindings(Map<String, Object> globalBindings) {
		if (globalBindings != null) {
			this.scriptEngineManager.setBindings(StandardScriptUtils.getBindings(globalBindings));
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		if (this.scriptEngineManager == null) {
			this.scriptEngineManager = new ScriptEngineManager(classLoader);
		}
	}


	@Override
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	public Object evaluate(ScriptSource script, Map<String, Object> argumentBindings) {
		ScriptEngine engine = getScriptEngine(script);
		try {
			if (CollectionUtils.isEmpty(argumentBindings)) {
				return engine.eval(script.getScriptAsString());
			}
			else {
				Bindings bindings = StandardScriptUtils.getBindings(argumentBindings);
				return engine.eval(script.getScriptAsString(), bindings);
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access script for ScriptEngine", ex);
		}
		catch (ScriptException ex) {
			throw new ScriptCompilationException(script, new StandardScriptEvalException(ex));
		}
	}

	/**
	 * 获取用于给定脚本的JSR-223 ScriptEngine.
	 * 
	 * @param script 要评估的脚本
	 * 
	 * @return the ScriptEngine (never {@code null})
	 */
	protected ScriptEngine getScriptEngine(ScriptSource script) {
		if (this.scriptEngineManager == null) {
			this.scriptEngineManager = new ScriptEngineManager();
		}

		if (StringUtils.hasText(this.engineName)) {
			return StandardScriptUtils.retrieveEngineByName(this.scriptEngineManager, this.engineName);
		}
		else if (script instanceof ResourceScriptSource) {
			Resource resource = ((ResourceScriptSource) script).getResource();
			String extension = StringUtils.getFilenameExtension(resource.getFilename());
			if (extension == null) {
				throw new IllegalStateException(
						"No script language defined, and no file extension defined for resource: " + resource);
			}
			ScriptEngine engine = this.scriptEngineManager.getEngineByExtension(extension);
			if (engine == null) {
				throw new IllegalStateException("No matching engine found for file extension '" + extension + "'");
			}
			return engine;
		}
		else {
			throw new IllegalStateException(
					"No script language defined, and no resource associated with script: " + script);
		}
	}

}
