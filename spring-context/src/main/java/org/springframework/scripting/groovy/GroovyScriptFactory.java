package org.springframework.scripting.groovy;

import java.io.IOException;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.Script;

/**
 * Groovy脚本的{@link org.springframework.scripting.ScriptFactory}实现.
 *
 * <p>通常与{@link org.springframework.scripting.support.ScriptFactoryPostProcessor}结合使用;
 * 请参阅后者的javadoc以获取配置示例.
 *
 * <p>Note: Spring 4.0支持Groovy 1.8及更高版本.
 */
public class GroovyScriptFactory implements ScriptFactory, BeanFactoryAware, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	private GroovyObjectCustomizer groovyObjectCustomizer;

	private CompilerConfiguration compilerConfiguration;

	private GroovyClassLoader groovyClassLoader;

	private Class<?> scriptClass;

	private Class<?> scriptResultClass;

	private CachedResultHolder cachedResult;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * <p>不需要在这里指定脚本接口, 因为Groovy脚本本身定义了它的Java接口.
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 */
	public GroovyScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
	}

	/**
	 * 指定一个策略接口, 该接口可以创建自定义MetaClass以提供缺少的方法, 并以其他方式更改对象的行为.
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * @param groovyObjectCustomizer 一个自定义器, 可以设置自定义元类或对此工厂创建的GroovyObject进行其他更改 (may be {@code null})
	 */
	public GroovyScriptFactory(String scriptSourceLocator, GroovyObjectCustomizer groovyObjectCustomizer) {
		this(scriptSourceLocator);
		this.groovyObjectCustomizer = groovyObjectCustomizer;
	}

	/**
	 * 指定一个策略接口, 该接口可以创建自定义MetaClass以提供缺少的方法, 并以其他方式更改对象的行为.
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * @param compilerConfiguration 要应用于GroovyClassLoader的自定义编译器配置 (may be {@code null})
	 */
	public GroovyScriptFactory(String scriptSourceLocator, CompilerConfiguration compilerConfiguration) {
		this(scriptSourceLocator);
		this.compilerConfiguration = compilerConfiguration;
	}

	/**
	 * 指定一个策略接口, 可以在底层GroovyClassLoader中自定义Groovy的编译过程.
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * @param compilationCustomizers 一个或多个要应用于GroovyClassLoader编译器配置的自定义器
	 */
	public GroovyScriptFactory(String scriptSourceLocator, CompilationCustomizer... compilationCustomizers) {
		this(scriptSourceLocator);
		if (!ObjectUtils.isEmpty(compilationCustomizers)) {
			this.compilerConfiguration = new CompilerConfiguration();
			this.compilerConfiguration.addCompilationCustomizers(compilationCustomizers);
		}
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			((ConfigurableListableBeanFactory) beanFactory).ignoreDependencyType(MetaClass.class);
		}
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.groovyClassLoader = buildGroovyClassLoader(classLoader);
	}

	/**
	 * 返回此脚本工厂使用的GroovyClassLoader.
	 */
	public GroovyClassLoader getGroovyClassLoader() {
		synchronized (this.scriptClassMonitor) {
			if (this.groovyClassLoader == null) {
				this.groovyClassLoader = buildGroovyClassLoader(ClassUtils.getDefaultClassLoader());
			}
			return this.groovyClassLoader;
		}
	}

	/**
	 * 为给定的{@code ClassLoader}构建{@link GroovyClassLoader}.
	 * 
	 * @param classLoader 用于构建GroovyClassLoader的ClassLoader
	 */
	protected GroovyClassLoader buildGroovyClassLoader(ClassLoader classLoader) {
		return (this.compilerConfiguration != null ?
				new GroovyClassLoader(classLoader, this.compilerConfiguration) : new GroovyClassLoader(classLoader));
	}


	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	/**
	 * Groovy脚本自己确定它们的接口, 因此不需要在这里显式地公开接口.
	 * 
	 * @return {@code null} always
	 */
	@Override
	public Class<?>[] getScriptInterfaces() {
		return null;
	}

	/**
	 * Groovy脚本不需要配置接口, 因为它们将其setter公开为public方法.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return false;
	}


	/**
	 * 通过GroovyClassLoader加载和解析Groovy脚本.
	 */
	@Override
	public Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				Class<?> scriptClassToExecute;
				this.wasModifiedForTypeCheck = false;

				if (this.cachedResult != null) {
					Object result = this.cachedResult.object;
					this.cachedResult = null;
					return result;
				}

				if (this.scriptClass == null || scriptSource.isModified()) {
					// New script content...
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// Groovy脚本, 可能创建一个实例: 执行它.
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						return result;
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				scriptClassToExecute = this.scriptClass;

				// 在同步块之外重新执行.
				return executeScript(scriptSource, scriptClassToExecute);
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				if (this.scriptClass == null || scriptSource.isModified()) {
					// New script content...
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = getGroovyClassLoader().parseClass(
							scriptSource.getScriptAsString(), scriptSource.suggestedClassName());

					if (Script.class.isAssignableFrom(this.scriptClass)) {
						// A Groovy script, probably creating an instance: let's execute it.
						Object result = executeScript(scriptSource, this.scriptClass);
						this.scriptResultClass = (result != null ? result.getClass() : null);
						this.cachedResult = new CachedResultHolder(result);
					}
					else {
						this.scriptResultClass = this.scriptClass;
					}
				}
				return this.scriptResultClass;
			}
			catch (CompilationFailedException ex) {
				this.scriptClass = null;
				this.scriptResultClass = null;
				this.cachedResult = null;
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		synchronized (this.scriptClassMonitor) {
			return (scriptSource.isModified() || this.wasModifiedForTypeCheck);
		}
	}


	/**
	 * 实例化给定的Groovy脚本类并在必要时运行它.
	 * 
	 * @param scriptSource 底层脚本的源
	 * @param scriptClass Groovy脚本类
	 * 
	 * @return 结果对象 (脚本类的实例, 或运行脚本实例的结果)
	 * @throws ScriptCompilationException 在实例化失败的情况下
	 */
	protected Object executeScript(ScriptSource scriptSource, Class<?> scriptClass) throws ScriptCompilationException {
		try {
			GroovyObject goo = (GroovyObject) scriptClass.newInstance();

			if (this.groovyObjectCustomizer != null) {
				// 允许元类和其他自定义.
				this.groovyObjectCustomizer.customize(goo);
			}

			if (goo instanceof Script) {
				// Groovy脚本, 可能创建一个实例: 执行它.
				return ((Script) goo).run();
			}
			else {
				// 脚本类的实例: 返回它.
				return goo;
			}
		}
		catch (InstantiationException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Unable to instantiate Groovy script class: " + scriptClass.getName(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new ScriptCompilationException(
					scriptSource, "Could not access Groovy script constructor: " + scriptClass.getName(), ex);
		}
	}


	@Override
	public String toString() {
		return "GroovyScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}


	/**
	 * 包含临时缓存结果对象的包装器.
	 */
	private static class CachedResultHolder {

		public final Object object;

		public CachedResultHolder(Object object) {
			this.object = object;
		}
	}
}
