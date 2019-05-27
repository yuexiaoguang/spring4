package org.springframework.jms.listener;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.jms.Connection;
import javax.jms.JMSException;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.util.ClassUtils;

/**
 * 需要基于JMS连接实现监听的所有容器的公共基类 (共享或新获取).
 * 从{@link org.springframework.jms.support.JmsAccessor}基类继承基本的Connection和Session配置处理.
 *
 * <p>此类提供基本生命周期管理, 特别是管理共享JMS连接.
 * 子类应插入此生命周期, 实现{@link #sharedConnectionEnabled()}以及{@link #doInitialize()} 和 {@link #doShutdown()}模板方法.
 *
 * <p>此基类不假设任何特定的监听器程序模型或监听器调用者机制.
 * 它只提供了在JMS Connection/Session上运行的任何类型的基于JMS的监听机制所需的通用运行时生命周期管理.
 *
 * <p>有关具体的监听器编程模型, 请查看{@link AbstractMessageListenerContainer}子类.
 * 有关具体的侦听器调用者机制, 请查看{@link DefaultMessageListenerContainer}类.
 */
public abstract class AbstractJmsListeningContainer extends JmsDestinationAccessor
		implements BeanNameAware, DisposableBean, SmartLifecycle {

	private String clientId;

	private boolean autoStartup = true;

	private int phase = Integer.MAX_VALUE;

	private String beanName;

	private Connection sharedConnection;

	private boolean sharedConnectionStarted = false;

	protected final Object sharedConnectionMonitor = new Object();

	private boolean active = false;

	private volatile boolean running = false;

	private final List<Object> pausedTasks = new LinkedList<Object>();

	protected final Object lifecycleMonitor = new Object();


	/**
	 * 为此容器创建和使用的共享Connection指定JMS客户端ID.
	 * <p>请注意, 客户端ID在底层JMS提供者的所有活动连接中必须是唯一的.
	 * 此外, 只有在尚未分配原始ConnectionFactory的情况下才能分配客户端ID.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * 返回此容器创建和使用的共享Connection的JMS客户端ID.
	 */
	public String getClientId() {
		return this.clientId;
	}

	/**
	 * 设置是否在初始化后自动启动容器.
	 * <p>默认"true"; 设置为"false"以允许通过{@link #start()}方法手动启动.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * 指定应启动和停止此容器的阶段.
	 * 启动顺序从最低到最高, 关闭顺序与此相反.
	 * 默认情况下, 此值为 Integer.MAX_VALUE, 表示此容器尽可能晚地启动并尽快停止.
	 */
	public void setPhase(int phase) {
		this.phase = phase;
	}

	/**
	 * 返回此容器将启动和停止的阶段.
	 */
	@Override
	public int getPhase() {
		return this.phase;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回已在其包含bean工厂中分配的此侦听器容器的bean名称.
	 */
	protected final String getBeanName() {
		return this.beanName;
	}


	/**
	 * 委托给{@link #validateConfiguration()} 和 {@link #initialize()}.
	 */
	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		validateConfiguration();
		initialize();
	}

	/**
	 * 验证此容器的配置.
	 * <p>默认实现为空. 要在子类中重写.
	 */
	protected void validateConfiguration() {
	}

	/**
	 * 当BeanFactory销毁容器实例时调用{@link #shutdown()}.
	 */
	@Override
	public void destroy() {
		shutdown();
	}


	//-------------------------------------------------------------------------
	// Lifecycle methods for starting and stopping the container
	//-------------------------------------------------------------------------

	/**
	 * 初始化此容器.
	 * <p>创建一个JMS Connection, 启动{@link javax.jms.Connection}
	 * (如果{@link #setAutoStartup(boolean) "autoStartup"}尚未关闭), 并调用{@link #doInitialize()}.
	 * 
	 * @throws org.springframework.jms.JmsException 如果启动失败
	 */
	public void initialize() throws JmsException {
		try {
			synchronized (this.lifecycleMonitor) {
				this.active = true;
				this.lifecycleMonitor.notifyAll();
			}
			doInitialize();
		}
		catch (JMSException ex) {
			synchronized (this.sharedConnectionMonitor) {
				ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), this.autoStartup);
				this.sharedConnection = null;
			}
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * 停止共享Connection, 调用{@link #doShutdown()}, 然后关闭此容器.
	 * 
	 * @throws JmsException 如果关闭失败
	 */
	public void shutdown() throws JmsException {
		logger.debug("Shutting down JMS listener container");
		boolean wasRunning;
		synchronized (this.lifecycleMonitor) {
			wasRunning = this.running;
			this.running = false;
			this.active = false;
			this.pausedTasks.clear();
			this.lifecycleMonitor.notifyAll();
		}

		// 如有必要, 请尽早停止共享连接.
		if (wasRunning && sharedConnectionEnabled()) {
			try {
				stopSharedConnection();
			}
			catch (Throwable ex) {
				logger.debug("Could not stop JMS Connection on shutdown", ex);
			}
		}

		// 关闭调用器.
		try {
			doShutdown();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			if (sharedConnectionEnabled()) {
				synchronized (this.sharedConnectionMonitor) {
					ConnectionFactoryUtils.releaseConnection(this.sharedConnection, getConnectionFactory(), false);
					this.sharedConnection = null;
				}
			}
		}
	}

	/**
	 * 返回此容器当前是否处于活动状态, 即是否已设置但尚未关闭.
	 */
	public final boolean isActive() {
		synchronized (this.lifecycleMonitor) {
			return this.active;
		}
	}

	/**
	 * 启动此容器.
	 * 
	 * @throws JmsException 如果启动失败
	 */
	@Override
	public void start() throws JmsException {
		try {
			doStart();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	/**
	 * 启动共享Connection, 并通知所有调用者任务.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doStart() throws JMSException {
		// 如有必要, 可以延迟地建立共享 Connection.
		if (sharedConnectionEnabled()) {
			establishSharedConnection();
		}

		// 重新安排暂停的任务.
		synchronized (this.lifecycleMonitor) {
			this.running = true;
			this.lifecycleMonitor.notifyAll();
			resumePausedTasks();
		}

		// 启动共享Connection.
		if (sharedConnectionEnabled()) {
			startSharedConnection();
		}
	}

	/**
	 * 停止此容器.
	 * 
	 * @throws JmsException 如果停止失败
	 */
	@Override
	public void stop() throws JmsException {
		try {
			doStop();
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	/**
	 * 通知所有调用者任务并停止共享连接.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void doStop() throws JMSException {
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.lifecycleMonitor.notifyAll();
		}

		if (sharedConnectionEnabled()) {
			stopSharedConnection();
		}
	}

	/**
	 * 确定此容器当前是否正在运行, 即它是否已启动但尚未停止.
	 */
	@Override
	public final boolean isRunning() {
		return (this.running && runningAllowed());
	}

	/**
	 * 检查通常是否允许此容器的监听器运行.
	 * <p>这个实现总是返回{@code true}; 默认的'运行'状态完全由{@link #start()} / {@link #stop()}决定.
	 * <p>子类可以重写此方法以检查阻止监听器实际运行的临时条件.
	 * 换句话说, 可以对'运行'状态应用进一步的限制, 如果这样的限制阻止监听器运行, 则返回{@code false}.
	 */
	protected boolean runningAllowed() {
		return true;
	}


	//-------------------------------------------------------------------------
	// Management of a shared JMS Connection
	//-------------------------------------------------------------------------

	/**
	 * 为此容器建立共享连接.
	 * <p>默认实现委托给{@link #createSharedConnection()}, 它会立即执行一次尝试并在失败时抛出异常.
	 * 可以重写以使恢复过程到位, 重试直到可以成功建立连接.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void establishSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				this.sharedConnection = createSharedConnection();
				logger.debug("Established shared JMS Connection");
			}
		}
	}

	/**
	 * 刷新此容器包含的共享Connection.
	 * <p>在启动时调用, 以及在调用器设置和/或执行期间发生基础结构异常之后调用.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected final void refreshSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			ConnectionFactoryUtils.releaseConnection(
					this.sharedConnection, getConnectionFactory(), this.sharedConnectionStarted);
			this.sharedConnection = null;
			this.sharedConnection = createSharedConnection();
			if (this.sharedConnectionStarted) {
				this.sharedConnection.start();
			}
		}
	}

	/**
	 * 为此容器创建共享连接.
	 * <p>默认实现创建标准连接, 并通过{@link #prepareSharedConnection}准备它.
	 * 
	 * @return 准备好的Connection
	 * @throws JMSException 如果创建失败
	 */
	protected Connection createSharedConnection() throws JMSException {
		Connection con = createConnection();
		try {
			prepareSharedConnection(con);
			return con;
		}
		catch (JMSException ex) {
			JmsUtils.closeConnection(con);
			throw ex;
		}
	}

	/**
	 * 准备给定的Connection, 将注册为此容器的共享连接.
	 * <p>默认实现设置指定的客户端ID.
	 * 子类可以覆盖它以应用进一步的设置.
	 * 
	 * @param connection 要准备的Connection
	 * 
	 * @throws JMSException 如果准备失败
	 */
	protected void prepareSharedConnection(Connection connection) throws JMSException {
		String clientId = getClientId();
		if (clientId != null) {
			connection.setClientID(clientId);
		}
	}

	/**
	 * 启动共享Connection.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void startSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = true;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.start();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection start exception - assuming already started: " + ex);
				}
			}
		}
	}

	/**
	 * 停止共享Connection.
	 * 
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected void stopSharedConnection() throws JMSException {
		synchronized (this.sharedConnectionMonitor) {
			this.sharedConnectionStarted = false;
			if (this.sharedConnection != null) {
				try {
					this.sharedConnection.stop();
				}
				catch (javax.jms.IllegalStateException ex) {
					logger.debug("Ignoring Connection stop exception - assuming already stopped: " + ex);
				}
			}
		}
	}

	/**
	 * 返回此容器维护的共享JMS Connection.
	 * 初始化后可用.
	 * 
	 * @return 共享的Connection (never {@code null})
	 * @throws IllegalStateException 如果此容器未维护共享 Connection, 或者尚未初始化Connection
	 */
	protected final Connection getSharedConnection() {
		if (!sharedConnectionEnabled()) {
			throw new IllegalStateException(
					"This listener container does not maintain a shared Connection");
		}
		synchronized (this.sharedConnectionMonitor) {
			if (this.sharedConnection == null) {
				throw new SharedConnectionNotInitializedException(
						"This listener container's shared Connection has not been initialized yet");
			}
			return this.sharedConnection;
		}
	}


	//-------------------------------------------------------------------------
	// Management of paused tasks
	//-------------------------------------------------------------------------

	/**
	 * 获取给定的任务对象并重新安排它, 如果此容器当前正在运行, 则立即重新安排, 或者稍后重新启动此容器.
	 * <p>如果此容器已关闭, 则根本不会重新安排任务.
	 * 
	 * @param task 要重新安排的任务对象
	 * 
	 * @return 是否已重新安排任务 (立即或重新启动此容器)
	 */
	protected final boolean rescheduleTaskIfNecessary(Object task) {
		if (this.running) {
			try {
				doRescheduleTask(task);
			}
			catch (RuntimeException ex) {
				logRejectedTask(task, ex);
				this.pausedTasks.add(task);
			}
			return true;
		}
		else if (this.active) {
			this.pausedTasks.add(task);
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * 尝试恢复所有暂停的任务.
	 * 重新安排失败的任务只是保持暂停模式.
	 */
	protected void resumePausedTasks() {
		synchronized (this.lifecycleMonitor) {
			if (!this.pausedTasks.isEmpty()) {
				for (Iterator<?> it = this.pausedTasks.iterator(); it.hasNext();) {
					Object task = it.next();
					try {
						doRescheduleTask(task);
						it.remove();
						if (logger.isDebugEnabled()) {
							logger.debug("Resumed paused task: " + task);
						}
					}
					catch (RuntimeException ex) {
						logRejectedTask(task, ex);
						// 将任务保持在暂停模式...
					}
				}
			}
		}
	}

	/**
	 * 确定当前暂停的任务的数量.
	 */
	public int getPausedTaskCount() {
		synchronized (this.lifecycleMonitor) {
			return this.pausedTasks.size();
		}
	}

	/**
	 * 立即重新安排给定的任务对象.
	 * <p>如果他们曾调用{@code rescheduleTaskIfNecessary}, 则由子类实现.
	 * 此实现抛出UnsupportedOperationException.
	 * 
	 * @param task 要重新安排的任务对象
	 */
	protected void doRescheduleTask(Object task) {
		throw new UnsupportedOperationException(
				ClassUtils.getShortName(getClass()) + " does not support rescheduling of tasks");
	}

	/**
	 * 记录被{@link #doRescheduleTask}拒绝的任务.
	 * <p>默认实现只是在调试级别记录相应的消息.
	 * 
	 * @param task 被拒绝的任务对象
	 * @param ex 从{@link #doRescheduleTask}抛出的异常
	 */
	protected void logRejectedTask(Object task, RuntimeException ex) {
		if (logger.isDebugEnabled()) {
			logger.debug("Listener container task [" + task + "] has been rejected and paused: " + ex);
		}
	}


	//-------------------------------------------------------------------------
	// Template methods to be implemented by subclasses
	//-------------------------------------------------------------------------

	/**
	 * 返回此容器基类是否应维护共享JMS连接.
	 */
	protected abstract boolean sharedConnectionEnabled();

	/**
	 * 在此容器中注册任何调用者.
	 * <p>子类需要为其特定的调用者管理过程实现此方法.
	 * <p>此时已经启动了共享JMS连接.
	 * 
	 * @throws JMSException 如果注册失败
	 */
	protected abstract void doInitialize() throws JMSException;

	/**
	 * 关闭注册的调用者.
	 * <p>子类需要为其特定的调用者管理过程实现此方法.
	 * <p>共享JMS Connection, <i>之后</i>将自动关闭.
	 * 
	 * @throws JMSException 如果关闭失败
	 */
	protected abstract void doShutdown() throws JMSException;


	/**
	 * 表示此容器的共享JMS连接的初始设置失败的异常.
	 * 这表明调用者需要在首次访问时自己建立共享连接.
	 */
	@SuppressWarnings("serial")
	public static class SharedConnectionNotInitializedException extends RuntimeException {

		protected SharedConnectionNotInitializedException(String msg) {
			super(msg);
		}
	}
}
