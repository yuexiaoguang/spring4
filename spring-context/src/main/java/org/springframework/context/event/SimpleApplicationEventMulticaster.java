package org.springframework.context.event;

import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;
import org.springframework.util.ErrorHandler;

/**
 * 简单实现{@link ApplicationEventMulticaster}接口.
 *
 * <p>将所有事件多播到所有已注册的监听器, 将其留给监听器以忽略他们不感​​兴趣的事件.
 * 监听器通常会对传入的事件对象执行相应的{@code instanceof}检查.
 *
 * <p>默认情况下, 在调用线程中调用所有监听器.
 * 这允许恶意监听器阻塞整个应用程序的危险, 但增加了最小的开销.
 * 指定备用任务执行器以使监听器在不同的线程中执行, 例如从线程池中执行.
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	private Executor taskExecutor;

	private ErrorHandler errorHandler;


	public SimpleApplicationEventMulticaster() {
	}

	public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}


	/**
	 * 设置自定义执行器 (通常是{@link org.springframework.core.task.TaskExecutor}) 以调用每个监听器.
	 * <p>默认等效于 {@link org.springframework.core.task.SyncTaskExecutor}, 在调用线程中同步执行所有监听器.
	 * <p>考虑在此处指定异步任务执行器, 以便在所有监听器都已执行之前不阻塞调用者.
	 * 但是, 请注意异步执行不会参与调用者的线程上下文 (类加载器, 事务关联), 除非TaskExecutor明确支持它.
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回此多播器的当前任务执行器.
	 */
	protected Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * 如果从监听器抛出异常, 请设置{@link ErrorHandler}以进行调用.
	 * <p>默认值为none, 监听器异常停止当前多播并传播到当前事件的发布者.
	 * 如果指定了{@linkplain #setTaskExecutor 任务执行器}, 则每个单独的监听器异常将传播到执行器, 但不一定会停止执行其他监听器.
	 * <p>考虑设置一个捕获和记录异常的{@link ErrorHandler}实现
	 * (a la {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER})
	 * 或者在传播异常的同时记录异常的实现
	 * (e.g. {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}).
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回此多播器的当前错误处理器.
	 */
	protected ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}


	@Override
	public void multicastEvent(ApplicationEvent event) {
		multicastEvent(event, resolveDefaultEventType(event));
	}

	@Override
	public void multicastEvent(final ApplicationEvent event, ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		for (final ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			Executor executor = getTaskExecutor();
			if (executor != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						invokeListener(listener, event);
					}
				});
			}
			else {
				invokeListener(listener, event);
			}
		}
	}

	private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
		return ResolvableType.forInstance(event);
	}

	/**
	 * 使用给定的事件调用给定的侦听器.
	 * 
	 * @param listener 要调用的ApplicationListener
	 * @param event 要传播的当前事件
	 */
	protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			try {
				doInvokeListener(listener, event);
			}
			catch (Throwable err) {
				errorHandler.handleError(err);
			}
		}
		else {
			doInvokeListener(listener, event);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
		try {
			listener.onApplicationEvent(event);
		}
		catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || matchesClassCastMessage(msg, event.getClass())) {
				// 可能是lambda定义的监听器, 我们无法解析泛型事件类型 -> 让我们抑制异常并只记录调试消息.
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Non-matching event type for listener: " + listener, ex);
				}
			}
			else {
				throw ex;
			}
		}
	}

	private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {
		// 在Java 8上, 消息以类名开头: "java.lang.String cannot be cast..."
		if (classCastMessage.startsWith(eventClass.getName())) {
			return true;
		}
		// 在Java 11上, 消息以 "class ..." a.k.a. Class.toString()开头
		if (classCastMessage.startsWith(eventClass.toString())) {
			return true;
		}
		// 在Java 9上, 消息用于包含模块名称: "java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = classCastMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}
		// Assuming an unrelated class cast failure...
		return false;
	}

}
