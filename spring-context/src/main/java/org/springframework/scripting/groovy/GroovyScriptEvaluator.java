package org.springframework.scripting.groovy;

import java.io.IOException;
import java.util.Map;

import groovy.lang.Binding;
import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptEvaluator;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * 基于Groovy的Spring {@link ScriptEvaluator}策略接口的实现.
 */
public class GroovyScriptEvaluator implements ScriptEvaluator, BeanClassLoaderAware {

	private ClassLoader classLoader;

	private CompilerConfiguration compilerConfiguration = new CompilerConfiguration();


	public GroovyScriptEvaluator() {
	}

	/**
	 * @param classLoader 用作{@link GroovyShell}的父级的ClassLoader
	 */
	public GroovyScriptEvaluator(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 为此评估器设置自定义编译器配置.
	 */
	public void setCompilerConfiguration(CompilerConfiguration compilerConfiguration) {
		this.compilerConfiguration =
				(compilerConfiguration != null ? compilerConfiguration : new CompilerConfiguration());
	}

	/**
	 * 返回此评估器的编译器配置 (never {@code null}).
	 */
	public CompilerConfiguration getCompilerConfiguration() {
		return this.compilerConfiguration;
	}

	/**
	 * 设置一个或多个自定义器以应用于此评估器的编译器配置.
	 * <p>请注意, 这会修改此评估器持有的共享编译器配置.
	 */
	public void setCompilationCustomizers(CompilationCustomizer... compilationCustomizers) {
		this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	@Override
	public Object evaluate(ScriptSource script) {
		return evaluate(script, null);
	}

	@Override
	public Object evaluate(ScriptSource script, Map<String, Object> arguments) {
		GroovyShell groovyShell = new GroovyShell(
				this.classLoader, new Binding(arguments), this.compilerConfiguration);
		try {
			String filename = (script instanceof ResourceScriptSource ?
					((ResourceScriptSource) script).getResource().getFilename() : null);
			if (filename != null) {
				return groovyShell.evaluate(script.getScriptAsString(), filename);
			}
			else {
				return groovyShell.evaluate(script.getScriptAsString());
			}
		}
		catch (IOException ex) {
			throw new ScriptCompilationException(script, "Cannot access Groovy script", ex);
		}
		catch (GroovyRuntimeException ex) {
			throw new ScriptCompilationException(script, ex);
		}
	}

}
