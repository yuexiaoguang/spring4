package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 构建并公开 {@link SimpleInstrumentableClassLoader}的{@code LoadTimeWeaver}.
 *
 * <p>主要用于测试环境, 在新环境中, 对新创建的{@code ClassLoader}实例执行所有类转换就足够了.
 */
public class SimpleLoadTimeWeaver implements LoadTimeWeaver {

	private final SimpleInstrumentableClassLoader classLoader;


	/**
	 * 为当前上下文{@code ClassLoader}创建一个新的{@code SimpleLoadTimeWeaver}.
	 */
	public SimpleLoadTimeWeaver() {
		this.classLoader = new SimpleInstrumentableClassLoader(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 为给定的{@code ClassLoader}创建一个新的{@code SimpleLoadTimeWeaver}.
	 * 
	 * @param classLoader 在其之上构建一个简单的可检测的{@code ClassLoader}的{@code ClassLoader}
	 */
	public SimpleLoadTimeWeaver(SimpleInstrumentableClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		this.classLoader.addTransformer(transformer);
	}

	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	/**
	 * 此实现构建一个{@link SimpleThrowawayClassLoader}.
	 */
	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

}
