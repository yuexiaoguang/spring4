package org.springframework.context;

/**
 * {@link Lifecycle}接口的扩展, 用于那些需要在ApplicationContext按特定顺序刷新和/或关闭时启动的对象.
 * {@link #isAutoStartup()}返回值指示是否应在上下文刷新时启动此对象.
 * 回调接受 {@link #stop(Runnable)} 方法对于具有异步关闭过程的对象很有用.
 * 此接口的任何实现都必须在关闭完成时调用回调的 run() 方法, 以避免整个ApplicationContext关闭时出现不必要的延迟.
 *
 * <p>此接口扩展 {@link Phased}, {@link #getPhase()} 方法的返回值指示应该启动和停止此Lifecycle组件的阶段.
 * 启动过程从最低阶段值开始, 以最高阶段值结束 (Integer.MIN_VALUE 是最低, Integer.MAX_VALUE 是最高).
 * 关闭过程将应用相反的顺序. 具有相同值的任何组件将在同一阶段中任意排序.
 *
 * <p>Example: 如果组件B依赖于已经启动的组件A, 那么组件A应该具有比组件B更低的阶段值.
 * 在关闭过程中, 组件B将在组件A之前停止.
 *
 * <p>任何明确的“依赖”关系都将优先于阶段顺序, 以便bean始终在其依赖之后启动, 并始终在其依赖之前停止.
 *
 * <p>上下文中未实现SmartLifecycle的生命周期组件都将被视为具有0的阶段值.
 * 这样, 如果SmartLifecycle实施具有负的阶段值, 则可以在这些生命周期组件之前启动,
 * 或者如果它具有正的阶段值, 它可以在这些组件之后启动.
 *
 * <p>请注意, 由于SmartLifecycle中的自动启动支持, 在任何情况下, SmartLifecycle bean实例将在应用程序上下文启动时初始化.
 * 因此, bean定义lazy-init标志对SmartLifecycle bean的实际影响非常有限.
 */
public interface SmartLifecycle extends Lifecycle, Phased {

	/**
	 * 如果在包含{@link ApplicationContext}被刷新时, 容器应该自动启动此{@code Lifecycle}组件, 则返回{@code true}.
	 * <p>值{@code false}表示该组件旨在通过显式 {@link #start()} 调用启动, 类似于普通的{@link Lifecycle}实现.
	 */
	boolean isAutoStartup();

	/**
	 * 表示如果Lifecycle组件当前正在运行, 则必须停止该组件.
	 * {@link LifecycleProcessor}使用提供的回调来支持, 具有公共关闭顺序值的所有组件, 有序且可能并发的关闭.
	 * 必须在{@code SmartLifecycle}组件确实停止后执行回调.
	 * <p>{@link LifecycleProcessor}将仅调用{@code stop}方法的此变体;
	 * i.e. 除非在此方法的实现中明确委托, 否则不会为{@code SmartLifecycle}实现调用{@link Lifecycle#stop()}.
	 */
	void stop(Runnable callback);

}
