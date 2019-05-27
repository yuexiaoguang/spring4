package org.springframework.instrument.classloading.jboss;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.springframework.util.ReflectionUtils;

/**
 * 围绕JBoss 6类加载器方法 (通过反射发现和调用) 的反射包装器, 用于加载时织入.
 */
class JBossMCAdapter implements JBossClassLoaderAdapter {

	private static final String LOADER_NAME = "org.jboss.classloader.spi.base.BaseClassLoader";

	private static final String TRANSLATOR_NAME = "org.jboss.util.loading.Translator";


	private final ClassLoader classLoader;

	private final Object target;

	private final Class<?> translatorClass;

	private final Method addTranslator;


	public JBossMCAdapter(ClassLoader classLoader) {
		try {
			// Resolve BaseClassLoader.class
			Class<?> clazzLoaderType = classLoader.loadClass(LOADER_NAME);

			ClassLoader clazzLoader = null;
			// 遍历层次结构以检测可检测感知的ClassLoader
			for (ClassLoader cl = classLoader; cl != null && clazzLoader == null; cl = cl.getParent()) {
				if (clazzLoaderType.isInstance(cl)) {
					clazzLoader = cl;
				}
			}

			if (clazzLoader == null) {
				throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: " +
						"A [" + LOADER_NAME + "] implementation is required.");
			}

			this.classLoader = clazzLoader;
			// Use the ClassLoader that loaded the ClassLoader to load the types for reflection purposes
			classLoader = clazzLoader.getClass().getClassLoader();

			// BaseClassLoader#getPolicy
			Method method = clazzLoaderType.getDeclaredMethod("getPolicy");
			ReflectionUtils.makeAccessible(method);
			this.target = method.invoke(this.classLoader);

			// Check existence of BaseClassLoaderPolicy#addTranslator(Translator)
			this.translatorClass = classLoader.loadClass(TRANSLATOR_NAME);
			this.addTranslator = this.target.getClass().getMethod("addTranslator", this.translatorClass);
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Could not initialize JBoss LoadTimeWeaver because the JBoss 6 API classes are not available", ex);
		}
	}

	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		InvocationHandler adapter = new JBossMCTranslatorAdapter(transformer);
		Object adapterInstance = Proxy.newProxyInstance(this.translatorClass.getClassLoader(),
				new Class<?>[] {this.translatorClass}, adapter);
		try {
			this.addTranslator.invoke(this.target, adapterInstance);
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not add transformer on JBoss 6 ClassLoader " + this.classLoader, ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

}
