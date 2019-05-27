package org.springframework.instrument.classloading;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.UsesJava7;

/**
 * 可用于加载类而不将它们带入父加载器的ClassLoader.
 * 旨在支持JPA "临时类加载器"要求, 但不支持特定于JPA.
 */
@UsesJava7
public class SimpleThrowawayClassLoader extends OverridingClassLoader {

	static {
		if (parallelCapableClassLoaderAvailable) {
			ClassLoader.registerAsParallelCapable();
		}
	}


	/**
	 * @param parent 用于构建一次性ClassLoader的ClassLoader
	 */
	public SimpleThrowawayClassLoader(ClassLoader parent) {
		super(parent);
	}

}
