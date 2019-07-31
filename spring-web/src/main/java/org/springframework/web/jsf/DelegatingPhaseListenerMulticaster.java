package org.springframework.web.jsf;

import java.util.Collection;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.web.context.WebApplicationContext;

/**
 * 委托给一个或多个Spring管理的来自Spring root WebApplicationContext的PhaseListener bean的JSF PhaseListener实现.
 *
 * <p>在{@code faces-config.xml}文件中配置此监听器多播器, 如下所示:
 *
 * <pre class="code">
 * &lt;application&gt;
 *   ...
 *   &lt;phase-listener&gt;
 *     org.springframework.web.jsf.DelegatingPhaseListenerMulticaster
 *   &lt;/phase-listener&gt;
 *   ...
 * &lt;/application&gt;</pre>
 *
 * 多播器将所有{@code beforePhase}和{@code afterPhase}事件委托给所有目标PhaseListener bean.
 * 默认情况下, 这些只是按类型获得:
 * 将获取并调用实现PhaseListener接口的Spring根WebApplicationContext中的所有bean.
 *
 * <p>Note: 此多播器的{@code getPhaseId()}方法将始终返回{@code ANY_PHASE}.
 * <b>目标监听器bean公开的阶段id将被忽略; 所有事件都将传播给所有监听器.</b>
 *
 * <p>可以对此多播器进行子类化以更改用于获取监听器bean的策略, 或者更改用于访问ApplicationContext的策略
 * (通常通过{@link FacesContextUtils#getWebApplicationContext(FacesContext)}获取).
 */
@SuppressWarnings("serial")
public class DelegatingPhaseListenerMulticaster implements PhaseListener {

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.ANY_PHASE;
	}

	@Override
	public void beforePhase(PhaseEvent event) {
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			listener.beforePhase(event);
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
		for (PhaseListener listener : getDelegates(event.getFacesContext())) {
			listener.afterPhase(event);
		}
	}


	/**
	 * 从Spring root WebApplicationContext获取委托PhaseListener bean.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return PhaseListener对象的集合
	 */
	protected Collection<PhaseListener> getDelegates(FacesContext facesContext) {
		ListableBeanFactory bf = getBeanFactory(facesContext);
		return BeanFactoryUtils.beansOfTypeIncludingAncestors(bf, PhaseListener.class, true, false).values();
	}

	/**
	 * 检索将bean名称解析委托给的Spring BeanFactory.
	 * <p>默认实现委托给{@code getWebApplicationContext}.
	 * 可以重写以提供任意ListableBeanFactory引用来解析; 通常, 这将是一个完整的Spring ApplicationContext.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return the Spring ListableBeanFactory (never {@code null})
	 */
	protected ListableBeanFactory getBeanFactory(FacesContext facesContext) {
		return getWebApplicationContext(facesContext);
	}

	/**
	 * 检索将bean名称解析委托给的Web应用程序上下文.
	 * <p>默认实现委托给 FacesContextUtils.
	 * 
	 * @param facesContext 当前的JSF上下文
	 * 
	 * @return Spring Web应用程序上下文 (never {@code null})
	 */
	protected WebApplicationContext getWebApplicationContext(FacesContext facesContext) {
		return FacesContextUtils.getRequiredWebApplicationContext(facesContext);
	}

}
