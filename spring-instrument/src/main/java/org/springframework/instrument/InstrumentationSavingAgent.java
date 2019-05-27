package org.springframework.instrument;

import java.lang.instrument.Instrumentation;

/**
 * Java代理程序, 用于保存JVM中的{@link Instrumentation}接口以供以后使用.
 */
public class InstrumentationSavingAgent {

	private static volatile Instrumentation instrumentation;


	/**
	 * 保存JVM公开的{@link Instrumentation}接口.
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * 保存JVM公开的{@link Instrumentation}接口.
	 * 使用Attach API动态加载此Agent需要此方法.
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
	}

	/**
	 * 返回JVM公开的{@link Instrumentation}接口.
	 * <p>请注意, 除非在JVM启动时实际指定了代理, 否则此代理类通常在类路径中不可用.
	 * 如果您打算对代理可用性进行条件检查, 考虑使用
	 * {@link org.springframework.instrument.classloading.InstrumentationLoadTimeWeaver#getInstrumentation()}
	 * - 这将在类路径中没有代理类的情况下工作.
	 * 
	 * @return JVM调用{@link #premain}或{@link #agentmain}方法时, 之前保存的{@code Instrumentation}实例;
	 * 如果在启动此JVM时未将此类用作Java代理, 或者未使用Attach API将其安装为代理, 则将为{@code null}.
	 */
	public static Instrumentation getInstrumentation() {
		return instrumentation;
	}

}
