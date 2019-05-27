package org.springframework.instrument.classloading.glassfish;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link LoadTimeWeaver}实现, 用于GlassFish的
 * {@code org.glassfish.api.deployment.InstrumentableClassLoader InstrumentableClassLoader}.
 *
 * <p>从Spring 4.0开始, 这个织入器支持GlassFish V3和V4.
 */
public class GlassFishLoadTimeWeaver implements LoadTimeWeaver {

	private static final String INSTRUMENTABLE_LOADER_CLASS_NAME =
			"org.glassfish.api.deployment.InstrumentableClassLoader";


	private final ClassLoader classLoader;

	private final Method addTransformerMethod;

	private final Method copyMethod;


	/**
	 * 使用默认的{@link ClassLoader 类加载器}.
	 */
	public GlassFishLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 要委托给用于织入的{@code ClassLoader}
	 */
	public GlassFishLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");

		Class<?> instrumentableLoaderClass;
		try {
			instrumentableLoaderClass = classLoader.loadClass(INSTRUMENTABLE_LOADER_CLASS_NAME);
			this.addTransformerMethod = instrumentableLoaderClass.getMethod("addTransformer", ClassFileTransformer.class);
			this.copyMethod = instrumentableLoaderClass.getMethod("copy");
		}
		catch (Throwable ex) {
			throw new IllegalStateException(
					"Could not initialize GlassFishLoadTimeWeaver because GlassFish API classes are not available", ex);
		}

		ClassLoader clazzLoader = null;
		// 通过遍历层次结构来检测转换感知的ClassLoader
		// (与在GlassFish中一样, Spring可以通过WebappClassLoader加载).
		for (ClassLoader cl = classLoader; cl != null && clazzLoader == null; cl = cl.getParent()) {
			if (instrumentableLoaderClass.isInstance(cl)) {
				clazzLoader = cl;
			}
		}

		if (clazzLoader == null) {
			throw new IllegalArgumentException(classLoader + " and its parents are not suitable ClassLoaders: A [" +
					instrumentableLoaderClass.getName() + "] implementation is required.");
		}

		this.classLoader = clazzLoader;
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		try {
			this.addTransformerMethod.invoke(this.classLoader, transformer);
		}
		catch (InvocationTargetException ex) {
			throw new IllegalStateException("GlassFish addTransformer method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke GlassFish addTransformer method", ex);
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
			throw new IllegalStateException("GlassFish copy method threw exception", ex.getCause());
		}
		catch (Throwable ex) {
			throw new IllegalStateException("Could not invoke GlassFish copy method", ex);
		}
	}

}
