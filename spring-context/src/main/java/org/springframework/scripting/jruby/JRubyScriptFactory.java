package org.springframework.scripting.jruby;

import java.io.IOException;
import java.lang.reflect.Method;

import org.jruby.RubyException;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.RaiseException;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.scripting.ScriptCompilationException;
import org.springframework.scripting.ScriptFactory;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * JRuby脚本的{@link org.springframework.scripting.ScriptFactory}实现.
 *
 * <p>通常与{@link org.springframework.scripting.support.ScriptFactoryPostProcessor}结合使用;
 * 请参阅后者的javadoc以获取配置示例.
 *
 * <p>Note: Spring 4.0支持JRuby 1.5及更高版本, 建议使用1.7.x..
 * 从Spring 4.2开始, 也支持JRuby 9.0.0.0,
 * 但主要是通过{@link org.springframework.scripting.support.StandardScriptFactory}.
 *
 * @deprecated in favor of JRuby support via the JSR-223 abstraction
 * ({@link org.springframework.scripting.support.StandardScriptFactory})
 */
@Deprecated
public class JRubyScriptFactory implements ScriptFactory, BeanClassLoaderAware {

	private static final Method getMessageMethod = ClassUtils.getMethodIfAvailable(RubyException.class, "getMessage");


	private final String scriptSourceLocator;

	private final Class<?>[] scriptInterfaces;

	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();


	/**
	 * @param scriptSourceLocator 指向脚本源的定位器.
	 * 由实际创建脚本的后处理器解释.
	 * @param scriptInterfaces 脚本对象应该实现的Java接口
	 */
	public JRubyScriptFactory(String scriptSourceLocator, Class<?>... scriptInterfaces) {
		Assert.hasText(scriptSourceLocator, "'scriptSourceLocator' must not be empty");
		Assert.notEmpty(scriptInterfaces, "'scriptInterfaces' must not be empty");
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
	 * JRuby脚本确实需要配置接口.
	 */
	@Override
	public boolean requiresConfigInterface() {
		return true;
	}

	/**
	 * 通过JRubyScriptUtils加载和解析JRuby脚本.
	 */
	@Override
	public Object getScriptedObject(ScriptSource scriptSource, Class<?>... actualInterfaces)
			throws IOException, ScriptCompilationException {
		try {
			return JRubyScriptUtils.createJRubyObject(
					scriptSource.getScriptAsString(), actualInterfaces, this.beanClassLoader);
		}
		catch (RaiseException ex) {
			String msg = null;
			RubyException rubyEx = ex.getException();
			if (rubyEx != null) {
				if (getMessageMethod != null) {
					// JRuby 9.1.7+ enforces access via getMessage() method
					msg = ReflectionUtils.invokeMethod(getMessageMethod, rubyEx).toString();
				}
				else {
					// JRuby 1.7.x: no accessor, just a public message field
					if (rubyEx.message != null){
						msg = rubyEx.message.toString();
					}
				}
			}
			throw new ScriptCompilationException(scriptSource, (msg != null ? msg : "Unexpected JRuby error"), ex);
		}
		catch (JumpException ex) {
			throw new ScriptCompilationException(scriptSource, ex);
		}
	}

	@Override
	public Class<?> getScriptedObjectType(ScriptSource scriptSource)
			throws IOException, ScriptCompilationException {

		return null;
	}

	@Override
	public boolean requiresScriptedObjectRefresh(ScriptSource scriptSource) {
		return scriptSource.isModified();
	}


	@Override
	public String toString() {
		return "JRubyScriptFactory: script source locator [" + this.scriptSourceLocator + "]";
	}

}
