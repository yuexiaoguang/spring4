package org.springframework.scripting.support;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * 处理JSR-223 {@link ScriptEngine}的常见操作.
 */
public abstract class StandardScriptUtils {

	/**
	 * 按名称从给定的{@link ScriptEngineManager}中检索{@link ScriptEngine},
	 * 委托给{@link ScriptEngineManager#getEngineByName}, 但如果找不到或初始化失败则抛出描述性异常.
	 * 
	 * @param scriptEngineManager 要使用的ScriptEngineManager
	 * @param engineName 引擎的名称
	 * 
	 * @return 对应的ScriptEngine (never {@code null})
	 * @throws IllegalArgumentException 如果没有找到匹配的引擎
	 * @throws IllegalStateException 如果所需的引擎未能初始化
	 */
	public static ScriptEngine retrieveEngineByName(ScriptEngineManager scriptEngineManager, String engineName) {
		ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
		if (engine == null) {
			Set<String> engineNames = new LinkedHashSet<String>();
			for (ScriptEngineFactory engineFactory : scriptEngineManager.getEngineFactories()) {
				List<String> factoryNames = engineFactory.getNames();
				if (factoryNames.contains(engineName)) {
					// Special case: getEngineByName返回null但引擎存在...
					// 假设它无法初始化 (ScriptEngineManager默默地吞下).
					// 如果它恰好正常初始化, 好吧, 但我们真的期待一个异常.
					try {
						engine = engineFactory.getScriptEngine();
						engine.setBindings(scriptEngineManager.getBindings(), ScriptContext.GLOBAL_SCOPE);
					}
					catch (Throwable ex) {
						throw new IllegalStateException("Script engine with name '" + engineName +
								"' failed to initialize", ex);
					}
				}
				engineNames.addAll(factoryNames);
			}
			throw new IllegalArgumentException("Script engine with name '" + engineName +
					"' not found; registered engine names: " + engineNames);
		}
		return engine;
	}

	static Bindings getBindings(Map<String, Object> bindings) {
		return (bindings instanceof Bindings ? (Bindings) bindings : new SimpleBindings(bindings));
	}

}
