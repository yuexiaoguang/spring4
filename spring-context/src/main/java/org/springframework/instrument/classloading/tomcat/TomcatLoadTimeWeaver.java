package org.springframework.instrument.classloading.tomcat;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.instrument.classloading.LoadTimeWeaver}实现,
 * 用于Tomcat的 {@code org.apache.tomcat.InstrumentableClassLoader}.
 * 遇到时还能够处理Spring的TomcatInstrumentableClassLoader.
 */
public class TomcatLoadTimeWeaver implements LoadTimeWeaver {

	private static final String INSTRUMENTABLE_LOADER_CLASS_NAME = "org.apache.tomcat.InstrumentableClassLoader";


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method copyMethod;


	public TomcatLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	public TomcatLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;

		Class<?> instrumentableLoaderClass;
		try {
			instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_LOADER_CLASS_NAME);
			if (!instrumentableLoaderClass.isInstance(classLoader)) {
				// 仍然可以是与约定兼容的ClassLoader的自定义变体
				instrumentableLoaderClass = classLoader.getClass();
			}
		}
		catch (ClassNotFoundException ex) {
			// 使用的是早期版本的Tomcat, 可能是Spring的TomcatInstrumentableClassLoader
			instrumentableLoaderClass = classLoader.getClass();
		}

		try {
			this.addTransformerMethod = instrumentableLoaderClass.getMethod("addTransformer", ClassFileTransformer.class);
			// 首先在InstrumentableClassLoader上检查Tomcat的新copyWithoutTransformers
			Method copyMethod = ClassUtils.getMethodIfAvailable(instrumentableLoaderClass, "copyWithoutTransformers");
			if (copyMethod == null) {
				// Fallback: 期待TomcatInstrumentableClassLoader的getThrowawayClassLoader
				copyMethod = instrumentableLoaderClass.getMethod("getThrowawayClassLoader");
			}
			this.copyMethod = copyMethod;
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Could not initialize TomcatLoadTimeWeaver because Tomcat API classes are not available", ex);
		}
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformerMethod.invoke(this.classLoader, transformer);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("Tomcat addTransformer method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke Tomcat addTransformer method", ex);
		}
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		try {
			return new OverridingClassLoader(this.classLoader, (ClassLoader) this.copyMethod.invoke(this.classLoader));
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("Tomcat copy method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke Tomcat copy method", ex);
		}
	}

}
