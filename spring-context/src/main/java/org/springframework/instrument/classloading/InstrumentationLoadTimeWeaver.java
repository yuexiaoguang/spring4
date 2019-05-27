package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.springframework.instrument.InstrumentationSavingAgent;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 依赖于 VM {@link Instrumentation}的{@link LoadTimeWeaver}.
 *
 * <p>启动JVM, 指定要使用的Java代理, 如下所示:
 *
 * <p><code class="code">-javaagent:path/to/org.springframework.instrument.jar</code>
 *
 * <p>其中{@code org.springframework.instrument.jar}是一个包含{@link InstrumentationSavingAgent}类的JAR文件, 与Spring一起提供.
 *
 * <p>例如, 在Eclipse中, 将"Run configuration"的JVM args设置为:
 *
 * <p><code class="code">-javaagent:${project_loc}/lib/org.springframework.instrument.jar</code>
 */
public class InstrumentationLoadTimeWeaver implements LoadTimeWeaver {

	private static final boolean AGENT_CLASS_PRESENT = ClassUtils.isPresent(
			"org.springframework.instrument.InstrumentationSavingAgent",
			InstrumentationLoadTimeWeaver.class.getClassLoader());


	private final ClassLoader classLoader;

	private final Instrumentation instrumentation;

	private final List<ClassFileTransformer> transformers = new ArrayList<ClassFileTransformer>(4);


	/**
	 * 为默认的ClassLoader创建一个新的InstrumentationLoadTimeWeaver.
	 */
	public InstrumentationLoadTimeWeaver() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 注册的变换器应该适用于的ClassLoader
	 */
	public InstrumentationLoadTimeWeaver(ClassLoader classLoader) {
		Assert.notNull(classLoader, "ClassLoader must not be null");
		this.classLoader = classLoader;
		this.instrumentation = getInstrumentation();
	}


	@Override
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		FilteringClassFileTransformer actualTransformer =
				new FilteringClassFileTransformer(transformer, this.classLoader);
		synchronized (this.transformers) {
			if (this.instrumentation == null) {
				throw new IllegalStateException(
						"Must start with Java agent to use InstrumentationLoadTimeWeaver. See Spring documentation.");
			}
			this.instrumentation.addTransformer(actualTransformer);
			this.transformers.add(actualTransformer);
		}
	}

	/**
	 * 能够以这种方式在启动JVM时织入当前的类加载器, 因此可检测的类加载器将始终是当前的加载器.
	 */
	@Override
	public ClassLoader getInstrumentableClassLoader() {
		return this.classLoader;
	}

	/**
	 * 此实现总是返回 {@link SimpleThrowawayClassLoader}.
	 */
	@Override
	public ClassLoader getThrowawayClassLoader() {
		return new SimpleThrowawayClassLoader(getInstrumentableClassLoader());
	}

	/**
	 * 按注册顺序删除所有已注册的转换器.
	 */
	public void removeTransformers() {
		synchronized (this.transformers) {
			if (!this.transformers.isEmpty()) {
				for (int i = this.transformers.size() - 1; i >= 0; i--) {
					this.instrumentation.removeTransformer(this.transformers.get(i));
				}
				this.transformers.clear();
			}
		}
	}


	/**
	 * 检查Instrumentation实例是否可用于当前VM.
	 */
	public static boolean isInstrumentationAvailable() {
		return (getInstrumentation() != null);
	}

	/**
	 * 获取当前VM的Instrumentation实例.
	 * 
	 * @return Instrumentation实例, 或{@code null}
	 */
	private static Instrumentation getInstrumentation() {
		if (AGENT_CLASS_PRESENT) {
			return InstrumentationAccessor.getInstrumentation();
		}
		else {
			return null;
		}
	}


	/**
	 * 要避免InstrumentationSavingAgent依赖的内部类.
	 */
	private static class InstrumentationAccessor {

		public static Instrumentation getInstrumentation() {
			return InstrumentationSavingAgent.getInstrumentation();
		}
	}


	/**
	 * 仅将给定的目标转换器应用于特定ClassLoader的装饰器.
	 */
	private static class FilteringClassFileTransformer implements ClassFileTransformer {

		private final ClassFileTransformer targetTransformer;

		private final ClassLoader targetClassLoader;

		public FilteringClassFileTransformer(ClassFileTransformer targetTransformer, ClassLoader targetClassLoader) {
			this.targetTransformer = targetTransformer;
			this.targetClassLoader = targetClassLoader;
		}

		@Override
		public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
				ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

			if (!this.targetClassLoader.equals(loader)) {
				return null;
			}
			return this.targetTransformer.transform(
					loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
		}

		@Override
		public String toString() {
			return "FilteringClassFileTransformer for: " + this.targetTransformer.toString();
		}
	}

}
