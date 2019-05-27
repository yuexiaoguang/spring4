package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

/**
 * 定义将一个或多个 {@link ClassFileTransformer ClassFileTransformers}添加到 {@link ClassLoader}的约定.
 *
 * <p>实现可以在当前上下文{@code ClassLoader}上运行, 也可以公开自己的工具{@code ClassLoader}.
 */
public interface LoadTimeWeaver {

	/**
	 * 添加要应用的{@code ClassFileTransformer}.
	 * 
	 * @param transformer 要添加的{@code ClassFileTransformer}
	 */
	void addTransformer(ClassFileTransformer transformer);

	/**
	 * 通过基于用户定义的{@link ClassFileTransformer ClassFileTransformers}的AspectJ样式加载时织入,
	 * 返回支持检测的{@code ClassLoader}.
	 * <p>可能是当前的{@code ClassLoader}, 或者由此{@link LoadTimeWeaver}实例创建的{@code ClassLoader}.
	 * 
	 * @return 将根据已注册的变换器公开已检测的类的{@code ClassLoader}
	 */
	ClassLoader getInstrumentableClassLoader();

	/**
	 * 返回一次性 {@code ClassLoader}, 允许加载和检查类, 而不影响父级 {@code ClassLoader}.
	 * <p>不应该返回从{@link #getInstrumentableClassLoader()}调用返回的{@link ClassLoader}的相同实例.
	 * 
	 * @return 临时的一次性的{@code ClassLoader}; 应该为每个调用返回一个新实例, 没有现有状态
	 */
	ClassLoader getThrowawayClassLoader();

}
