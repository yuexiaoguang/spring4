package org.springframework.scripting.bsh;

import java.io.IOException;

import bsh.EvalError;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * BeanShell脚本的{@link org.springframework.scripting.ScriptFactory}实现.
 *
 * <p>通常与{@link org.springframework.scripting.support.ScriptFactoryPostProcessor}结合使用;
 * 请参阅后者的javadoc以获取配置示例.
 */
public class BshScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	private final String scriptSourceLocator;

	private final Class<?>[] scriptInterfaces;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	private Class<?> scriptClass;

	private final Object scriptClassMonitor = new Object();

	private boolean wasModifiedForTypeCheck = false;


	/**
	 * <p>使用此{@code BshScriptFactory}变体, 脚本需要声明一个完整的类或返回脚本对象的实际实例.
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 */
	public BshScriptFactory(String scriptSourceLocator) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = null;
	}

	/**
	 * <p>该脚本可以是一个需要生成相应代理的简单脚本 (实现指定的接口),
	 * 或声明一个完整的类或返回脚本对象的实际实例 (在这种情况下, 指定的接口, 如果有的话, 需要由该类/实例实现).
	 * 
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * @param scriptInterfaces 脚本对象应该实现的Java接口 (may be {@code null})
	 */
	public BshScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		this.scriptSourceLocator = scriptSourceLocator;
		this.scriptInterfaces = scriptInterfaces;
	}


	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}


	@Override
	public String getScriptSourceLocator() {
		return this.scriptSourceLocator;
	}

	@Override
	public Class<?>[] getScriptInterfaces() {
		return this.scriptInterfaces;
	}

	/**
	 * BeanShell脚本确实需要配置接口.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return true;
	}

	/**
	 * 通过{@link BshScriptUtils}加载并解析BeanShell脚本.
	 */
	@Override
	public Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {

		Class<?> clazz;

		try {
			synchronized (this.scriptClassMonitor) {
				boolean requiresScriptEvaluation = (this.wasModifiedForTypeCheck && this.scriptClass == null);
				this.wasModifiedForTypeCheck = false;

				if (scriptSource.isModified() || requiresScriptEvaluation) {
					// 新脚本内容: 检查它是否评估为一个类.
					Object result = BshScriptUtils.evaluateBshScript(
							scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
					if (result instanceof Class) {
						// A Class: 将在此处缓存Class并在synchronized块之外创建一个实例.
						this.scriptClass = (Class<?>) result;
					}
					else {
						// Not a Class: OK, 将通过稍后为每个调用评估脚本来创建BeanShell对象.
						// 对于第一次检查, 让我们简单地返回已经评估的对象.
						return result;
					}
				}
				clazz = this.scriptClass;
			}
		}
		catch (EvalError ex) {
			this.scriptClass = null;
			throw new ScriptCompilationException(scriptSource, ex);
		}

		if (clazz != null) {
			// A Class: 需要为每个调用创建一个实例.
			try {
				return clazz.newInstance();
			}
			catch (Throwable ex) {
				throw new ScriptCompilationException(
						scriptSource, "Could not instantiate script class: " + clazz.getName(), ex);
			}
		}
		else {
			// Not a Class: 需要为每次调用评估脚本.
			try {
				return BshScriptUtils.createBshObject(
						scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
			}
			catch (EvalError ex) {
				throw new ScriptCompilationException(scriptSource, ex);
			}
		}
	}

	@Override
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		synchronized (this.scriptClassMonitor) {
			try {
				if (scriptSource.isModified()) {
					// New script content: 检查它是否评估为一个类.
					this.wasModifiedForTypeCheck = true;
					this.scriptClass = BshScriptUtils.determineBshObjectType(
							scriptSource.getScriptAsString(), this.beanClassLoader);
				}
				return this.scriptClass;
			}
			catch (EvalError ex) {
				this.scriptClass = null;
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


	@Override
	public String toString() {
		return "BshScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
