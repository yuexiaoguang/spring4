package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.DecoratingClassLoader;
import org.springframework.core.OverridingClassLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 使用反射委托给具有转换钩子的底层ClassLoader的{@link LoadTimeWeaver}.
 * 底层的ClassLoader应该支持以下织入方法 (如{@link LoadTimeWeaver}接口中所定义):
 * <ul>
 * <li>{@code public void addTransformer(java.lang.instrument.ClassFileTransformer)}:
 * 用于在此ClassLoader上注册给定的ClassFileTransformer
 * <li>{@code public ClassLoader getThrowawayClassLoader()}:
 * 用于获取此ClassLoader的一次性类加载器 (optional;
 * 如果该方法不可用, ReflectiveLoadTimeWeaver将回退到SimpleThrowawayClassLoader)
 * </ul>
 *
 * <p>请注意, 上述方法必须位于可公开访问的类中, 虽然类本身不必对应用程序的类加载器可见.
 *
 * <p>当底层的ClassLoader实现加载到不同的类加载器本身时, 这个LoadTimeWeaver的反射特性特别有用
 * (例如Web应用程序看不到的应用程序服务器的类加载器).
 * 此LoadTimeWeaver适配器与底层ClassLoader之间没有直接的API依赖关系, 只是一个'松散'的方法约定.
 *
 * <p>这是与Spring的
 * {@link org.springframework.instrument.classloading.tomcat.TomcatInstrumentableClassLoader}结合使用的LoadTimeWeaver,
 * 用于Tomcat 5.0+以及Resin应用程序服务器版本3.1+.
 */
public class ReflectiveLoadTimeWeaver implements LoadTimeWeaver {

	private static final String ADD_TRANSFORMER_METHOD_NAME = "addTransformer";

	private static final String GET_THROWAWAY_CLASS_LOADER_METHOD_NAME = "getThrowawayClassLoader";

	private static final Log logger = LogFactory.getLog(ReflectiveLoadTimeWeaver.class);


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method getThrowawayClassLoaderMethod;


	/**
	 * 为当前上下文类加载器创建一个新的ReflectiveLoadTimeWeaver, <i>需要支持所需的织入方法</i>.
	 */
	public ReflectiveLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 委托给用于织入的{@code ClassLoader} (必须支持所需的织入方法).
	 * 
	 * @throws IllegalStateException 如果提供的{@code ClassLoader}不支持所需的织入方法
	 */
	public ReflectiveLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
		this.addTransformerMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), ADD_TRANSFORMER_METHOD_NAME, ClassFileTransformer.class);
		if (this.addTransformerMethod == null) {
			throw new IllegalStateException(
					"ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide an " +
					"'addTransformer(ClassFileTransformer)' method.");
		}
		this.getThrowawayClassLoaderMethod = ClassUtils.getMethodIfAvailable(
				this.classLoader.getClass(), GET_THROWAWAY_CLASS_LOADER_METHOD_NAME);
		// getThrowawayClassLoader方法是可选的
		if (this.getThrowawayClassLoaderMethod == null) {
			if (logger.isInfoEnabled()) {
				logger.info("The ClassLoader [" + classLoader.getClass().getName() + "] does NOT provide a " +
						"'getThrowawayClassLoader()' method; SimpleThrowawayClassLoader will be used instead.");
			}
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		ReflectionUtils.invokeMethod(this.addTransformerMethod, this.classLoader, transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		if (this.getThrowawayClassLoaderMethod != null) {
			ClassLoader target = (ClassLoader)
					ReflectionUtils.invokeMethod(this.getThrowawayClassLoaderMethod, this.classLoader);
			return (target instanceof DecoratingClassLoader ? target :
					new OverridingClassLoader(this.classLoader, target));
		}
		else {
			return new SimpleThrowawayClassLoader(this.classLoader);
		}
	}

}
