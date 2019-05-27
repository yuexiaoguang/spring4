package org.springframework.scripting;

import java.util.Map;

/**
 * Spring用于评估脚本的策略接口.
 *
 * <p>除了特定于语言的实现之外, Spring还提供了基于标准 {@code javax.script}包 (JSR-223)的版本:
 * {@link org.springframework.scripting.support.StandardScriptEvaluator}.
 */
public interface ScriptEvaluator {

	/**
	 * 评估给定的脚本.
	 * 
	 * @param script 用于评估脚本的ScriptSource
	 * 
	 * @return 脚本的返回值
	 * @throws ScriptCompilationException 如果评估器未能读取, 编译或评估脚本
	 */
	Object evaluate(ScriptSource script) throws ScriptCompilationException;

	/**
	 * 使用给定的参数评估给定的脚本.
	 * 
	 * @param script 用于评估脚本的ScriptSource
	 * @param arguments 要暴露给脚本的键值对, 通常作为脚本变量 (may be {@code null} or empty)
	 * 
	 * @return 脚本的返回值
	 * @throws ScriptCompilationException 如果评估器未能读取, 编译或评估脚本
	 */
	Object evaluate(ScriptSource script, Map<String, Object> arguments) throws ScriptCompilationException;

}
