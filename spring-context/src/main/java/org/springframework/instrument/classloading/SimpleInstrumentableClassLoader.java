package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.core.OverridingClassLoader;
import org.springframework.lang.UsesJava7;

/**
 * 可检测的{@code ClassLoader}的简单实现.
 *
 * <p>可用于测试和独立环境.
 */
@UsesJava7
public class SimpleInstrumentableClassLoader extends OverridingClassLoader {

	static {
		if (parallelCapableClassLoaderAvailable) {
			ClassLoader.registerAsParallelCapable();
		}
	}


	private final WeavingTransformer weavingTransformer;


	/**
	 * @param parent 用于构建可检测的ClassLoader的ClassLoader
	 */
	public SimpleInstrumentableClassLoader(ClassLoader parent) {
		super(parent);
		this.weavingTransformer = new WeavingTransformer(parent);
	}


	/**
	 * 添加要由此ClassLoader应用的 {@link ClassFileTransformer}.
	 * 
	 * @param transformer 要注册的{@link ClassFileTransformer}
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		this.weavingTransformer.addTransformer(transformer);
	}


	@Override
	protected byte[] transformIfNecessary(String name, byte[] bytes) {
		return this.weavingTransformer.transformIfNecessary(name, bytes);
	}

}
