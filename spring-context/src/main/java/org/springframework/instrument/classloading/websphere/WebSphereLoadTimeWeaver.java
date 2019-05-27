package org.springframework.instrument.classloading.websphere;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * WebSphere的可检测的ClassLoader的{@link LoadTimeWeaver}实现.
 * 与WebSphere 7以及8和9兼容.
 */
public class WebSphereLoadTimeWeaver implements LoadTimeWeaver {

	private final WebSphereClassLoaderAdapter classLoader;


	/**
	 * 使用默认的{@link ClassLoader 类加载器}.
	 */
	public WebSphereLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 要委托给用于织入的{@code ClassLoader}
	 */
	public WebSphereLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = new WebSphereClassLoaderAdapter(classLoader);
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader.getClassLoader();
	}

	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new OverridingClassLoader(this.classLoader.getClassLoader(),
				this.classLoader.getThrowawayClassLoader());
	}

}
