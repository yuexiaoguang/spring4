package org.springframework.web.jsf;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;

/**
 * 希望能够装饰原始NavigationHandler的JSF NavigationHandler实现的基类.
 *
 * <p>支持标准JSF样式的装饰 (通过构造函数参数),
 * 以及带有显式NavigationHandler参数的重载{@code handleNavigation}方法 (传入原始NavigationHandler).
 * 子类被迫实现这个重载的{@code handleNavigation}方法.
 * 标准JSF调用将自动委托给重载方法, 并将构造函数注入的NavigationHandler作为参数.
 */
public abstract class DecoratingNavigationHandler extends NavigationHandler {

	private NavigationHandler decoratedNavigationHandler;


	/**
	 * 没有固定原始NavigationHandler.
	 */
	protected DecoratingNavigationHandler() {
	}

	/**
	 * @param originalNavigationHandler 要装饰的原始的NavigationHandler
	 */
	protected DecoratingNavigationHandler(NavigationHandler originalNavigationHandler) {
		this.decoratedNavigationHandler = originalNavigationHandler;
	}

	/**
	 * 返回由此处理器装饰的固定原始NavigationHandler (即, 如果通过构造函数传入).
	 */
	public final NavigationHandler getDecoratedNavigationHandler() {
		return this.decoratedNavigationHandler;
	}


	/**
	 * 标准JSF {@code handleNavigation}方法的这种实现委托给重载的变体, 传入构造函数注入的NavigationHandler作为参数.
	 */
	@Override
	public final void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
		handleNavigation(facesContext, fromAction, outcome, this.decoratedNavigationHandler);
	}

	/**
	 * 具有显式NavigationHandler参数的特殊{@code handleNavigation}变体.
	 * 通过带有显式原始处理器的代码直接调用, 或者从标准{@code handleNavigation}方法调用, 作为普通JSF定义的NavigationHandler.
	 * <p>实现应该调用{@code callNextHandlerInChain}来委托给链中的下一个处理器.
	 * 这将始终调用最合适的下一个处理器 (see {@code callNextHandlerInChain} javadoc).
	 * 或者, 也可以直接调用装饰的NavigationHandler或传入的原始NavigationHandler;
	 * 然而, 就对链中潜在位置的反应而言, 这并不灵活.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * @param fromAction 已评估的动作绑定表达式用于检索指定的结果, 或{@code null} 如果结果是通过其他方式获取的
	 * @param outcome 先前调用的应用程序操作返回的逻辑结果 (可能是{@code null})
	 * @param originalNavigationHandler 原始NavigationHandler, 或{@code null}
	 */
	public abstract void handleNavigation(
			FacesContext facesContext, String fromAction, String outcome, NavigationHandler originalNavigationHandler);


	/**
	 * 当打算委托给NavigationHandler链中的下一个处理器时, 子类调用的方法.
	 * 将始终调用最合适的下一个处理器, 作为构造函数参数传入装饰的NavigationHandler,
	 * 或传递给此方法的原始NavigationHandler - 根据此实例在链中的位置.
	 * <p>将调用装饰的NavigationHandler指定为构造函数参数.
	 * 如果将DecoratingNavigationHandler作为目标, 则传递给此方法的原始NavigationHandler将传递给链中的下一个元素:
	 * 这确保了处理器链中最后一个元素可能委托回原始处理器的传播.
	 * 如果标准的NavigationHandler作为目标, 原始处理器将不会被传递; 在该场景中, 没有任何委托回到原始链接的可能性.
	 * <p>如果未装饰的NavigationHandler指定为构造函数参数, 则此实例是链中的最后一个元素.
	 * 因此, 此方法将调用传递给此方法的原始NavigationHandler.
	 * 如果没有传入原始的NavigationHandler (例如, 如果此实例是链中的最后一个元素,
	 * 标准NavigationHandlers作为早期元素), 则此方法对应于无操作.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * @param fromAction 已评估的动作绑定表达式, 用于检索指定的结果, 或{@code null} 如果结果是通过其他方式获取的
	 * @param outcome 先前调用的应用程序操作返回的逻辑结果 (可能是{@code null})
	 * @param originalNavigationHandler 原始的NavigationHandler, 或{@code null}
	 */
	protected final void callNextHandlerInChain(
			FacesContext facesContext, String fromAction, String outcome, NavigationHandler originalNavigationHandler) {

		NavigationHandler decoratedNavigationHandler = getDecoratedNavigationHandler();

		if (decoratedNavigationHandler instanceof DecoratingNavigationHandler) {
			// 通过构造函数参数指定的DecoratingNavigationHandler:
			// 使用传入的原始NavigationHandler调用它.
			DecoratingNavigationHandler decHandler = (DecoratingNavigationHandler) decoratedNavigationHandler;
			decHandler.handleNavigation(facesContext, fromAction, outcome, originalNavigationHandler);
		}
		else if (decoratedNavigationHandler != null) {
			// 通过构造函数参数指定的标准NavigationHandler:
			// 通过标准API调用它, 而不传入原始的NavigationHandler.
			// 被调用的处理器将无法重定向到原始处理器.
			decoratedNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		}
		else if (originalNavigationHandler != null) {
			// 没有通过构造函数参数指定的NavigationHandler:
			// 调用原始处理器, 标记此链的末尾.
			originalNavigationHandler.handleNavigation(facesContext, fromAction, outcome);
		}
	}

}
