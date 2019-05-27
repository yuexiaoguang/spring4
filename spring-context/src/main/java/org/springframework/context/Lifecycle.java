package org.springframework.context;

/**
 * 定义启动/停止生命周期控制方法的通用接口.
 * 这种情况的典型用例是控制异步处理.
 * <b>NOTE: 此接口并不意味着特定的自动启动语义. 考虑为此目的实现 {@link SmartLifecycle}.</b>
 *
 * <p>可以由两个组件 (通常是Spring上下文中定义的Spring bean) 和容器 (通常是Spring {@link ApplicationContext}本身)实现.
 * 容器会将开始/停止信号传播到每个容器中应用的所有组件, e.g. 用于运行时停止/重新启动场景中.
 *
 * <p>可用于通过JMX进行直接调用或管理操作.
 * 在后一种情况下, {@link org.springframework.jmx.export.MBeanExporter}通常会使用
 * {@link org.springframework.jmx.export.assembler.InterfaceBasedMBeanInfoAssembler}定义,
 * 将活动控制组件的可见性限制为Lifecycle接口.
 *
 * <p>请注意, 仅在<b>顶级单例bean</b>上支持Lifecycle接口.
 * 在任何其他组件上, Lifecycle接口将保持未检测状态, 因此将被忽略.
 * 另请注意, 扩展的{@link SmartLifecycle}接口提供了与应用程序上下文的启动和关闭阶段的集成.
 */
public interface Lifecycle {

	/**
	 * 启动此组件.
	 * <p>如果组件已在运行, 则不应抛出异常.
	 * <p>对于容器, 这会将启动信号传播到所有适用的组件.
	 */
	void start();

	/**
	 * 通常以同步方式停止此组件, 以便在此方法返回时组件完全停止.
	 * 当需要异步停止行为时, 请考虑实现{@link SmartLifecycle}及其{@code stop(Runnable)}变体.
	 * <p>请注意, 不能保证在销毁之前发出此停止通知:
	 * 在常规关闭时, {@code Lifecycle} bean将在传播一般销毁回调之前首先收到停止通知;
	 * 但是, 在上下文生命周期中的热刷新或中止刷新尝试时, 只会调用destroy方法.
	 * <p>如果组件尚未启动, 则不应抛出异常.
	 * <p>对于容器, 这会将停止信号传播到所有适用的组件.
	 */
	void stop();

	/**
	 * 检查此组件当前是否正在运行.
	 * <p>对于容器, 仅当应用的所有组件当前正在运行时, 才会返回{@code true}.
	 * 
	 * @return 组件当前是否正在运行
	 */
	boolean isRunning();

}
