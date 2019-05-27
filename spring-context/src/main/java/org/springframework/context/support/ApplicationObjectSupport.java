package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;

/**
 * 想要了解应用程序上下文的应用程序对象的便捷超类,
 * e.g. 用于自定义查找协作Bean或用于特定于上下文的资源访问.
 * 它保存应用程序上下文引用, 并提供初始化回调方法.
 * 此外, 它为消息查找提供了许多便利方法.
 *
 * <p>没有要求对此类进行子类化:
 * 如果您需要访问上下文, 它只会使事情变得更容易一些, e.g. 用于访问文件资源或消息源.
 * 请注意, 许多应用程序对象根本不需要了解应用程序上下文, 因为它们可以通过bean引用接收协作bean.
 *
 * <p>许多框架类都派生自此类, 特别是在Web支持中.
 */
public abstract class ApplicationObjectSupport implements ApplicationContextAware {

	/** Logger that is available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	/** 此对象运行的ApplicationContext */
	private ApplicationContext applicationContext;

	/** 便于消息访问的MessageSourceAccessor */
	private MessageSourceAccessor messageSourceAccessor;


	@Override
	public final void setApplicationContext(ApplicationContext context) throws BeansException {
		if (context == null && !isContextRequired()) {
			// Reset internal context state.
			this.applicationContext = null;
			this.messageSourceAccessor = null;
		}
		else if (this.applicationContext == null) {
			// 使用传入上下文进行初始化.
			if (!requiredContextClass().isInstance(context)) {
				throw new ApplicationContextException(
						"Invalid application context: needs to be of type [" + requiredContextClass().getName() + "]");
			}
			this.applicationContext = context;
			this.messageSourceAccessor = new MessageSourceAccessor(context);
			initApplicationContext(context);
		}
		else {
			// 如果传入相同的上下文, 则忽略重新初始化.
			if (this.applicationContext != context) {
				throw new ApplicationContextException(
						"Cannot reinitialize with different application context: current one is [" +
						this.applicationContext + "], passed-in one is [" + context + "]");
			}
		}
	}

	/**
	 * 确定此应用程序对象是否需要在ApplicationContext中运行.
	 * <p>默认是 "false". 可以重写以强制在上下文中运行
	 * (i.e. 如果在上下文之外, 则在访问器上抛出IllegalStateException).
	 */
	protected boolean isContextRequired() {
		return false;
	}

	/**
	 * 确定传递给{@code setApplicationContext}的任何上下文必须是其实例的上下文类.
	 * 可以在子类中重写.
	 */
	protected Class<?> requiredContextClass() {
		return ApplicationContext.class;
	}

	/**
	 * 子类可以覆盖此自定义初始化行为.
	 * 在设置上下文实例后, 由{@code setApplicationContext}调用.
	 * <p>Note: 不会在重新初始化上下文时调用, 而只会在首次初始化此对象的上下文引用时调用.
	 * <p>默认实现在没有ApplicationContext引用的情况下调用重载的 {@link #initApplicationContext()}方法.
	 * 
	 * @param context 包含ApplicationContext
	 * 
	 * @throws ApplicationContextException 初始化错误
	 * @throws BeansException 如果由ApplicationContext方法抛出
	 */
	protected void initApplicationContext(ApplicationContext context) throws BeansException {
		initApplicationContext();
	}

	/**
	 * 子类可以覆盖此自定义初始化行为.
	 * <p>默认实现为空. 由
	 * {@link #initApplicationContext(org.springframework.context.ApplicationContext)}调用.
	 * 
	 * @throws ApplicationContextException 初始化错误
	 * @throws BeansException 如果由ApplicationContext方法抛出
	 */
	protected void initApplicationContext() throws BeansException {
	}


	/**
	 * 返回与此对象关联的ApplicationContext.
	 * 
	 * @throws IllegalStateException 如果没有在ApplicationContext中运行
	 */
	public final ApplicationContext getApplicationContext() throws IllegalStateException {
		if (this.applicationContext == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.applicationContext;
	}

	/**
	 * 返回此对象使用的应用程序上下文的MessageSourceAccessor, 以便于访问消息.
	 * 
	 * @throws IllegalStateException 如果没有在ApplicationContext中运行
	 */
	protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
		if (this.messageSourceAccessor == null && isContextRequired()) {
			throw new IllegalStateException(
					"ApplicationObjectSupport instance [" + this + "] does not run in an ApplicationContext");
		}
		return this.messageSourceAccessor;
	}

}
