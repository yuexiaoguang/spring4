package org.springframework.web.portlet.mvc;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletSession;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.handler.PortletContentGenerator;
import org.springframework.web.portlet.util.PortletUtils;

/**
 * 控制器实现的便捷超类, 使用模板方法设计模式.
 *
 * <p>如{@link Controller Controller}接口中所述, 某些抽象基本控制器已经提供了许多功能.
 * AbstractController是最重要的抽象基础控制器之一, 提供基本功能, 如控制是否需要会话和渲染缓存.
 *
 * <p><b><a name="workflow">Workflow (<a href="Controller.html#workflow">和接口定义的那些</a>):</b><br>
 * <ol>
 * <li>如果这是一个操作请求, DispatcherPortlet将调用{@link #handleActionRequest handleActionRequest}一次,
 * 以执行此控制器定义的动作.</li>
 * <li>如果需要会话, 尝试获取它 (如果未找到则抛出PortletException).</li>
 * <li>调用方法{@link #handleActionRequestInternal handleActionRequestInternal},
 * (可选地围绕PortletSession上的调用进行同步), 这应该通过扩展类来覆盖, 以提供执行控制器所需操作的实际功能.
 * 这只会执行一次.</li>
 * <li>对于直接渲染请求或操作请求的渲染阶段 (假设渲染阶段调用相同的控制器 -- 请参阅下面的提示),
 * DispatcherPortlet将重复调用{@link #handleRenderRequest handleRenderRequest}, 以渲染此控制器定义的显示.</li>
 * <li>如果需要会话, 请尝试获取它 (如果没有找到则抛出PortletException).</li>
 * <li>它将控制cacheSeconds属性定义的缓存.</li>
 * <li>调用方法{@link #handleRenderRequestInternal handleRenderRequestInternal},
 * (可选地围绕PortletSession上的调用进行同步), 应该通过扩展类来覆盖,
 * 以提供返回{@link org.springframework.web.portlet.ModelAndView ModelAndView}对象的实际功能.
 * 当portal更新当前显示的页面时, 这将重复执行.</li>
 * </ol>
 *
 * <p><b><a name="config">暴露的配置属性</a>
 * (<a href="Controller.html#config">和接口定义的属性</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></th>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>此控制器是否需要会话才能处理请求.
 * 这确保了派生控制器可以 - 无需担心Nullpointers - 调用 request.getSession()来检索会话.
 * 如果在处理请求时找不到会话, 则将抛出PortletException</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>是否应该围绕PortletSession同步对{@code handleRenderRequestInternal}
 * 和{@code handleRenderRequestInternal}的调用, 以序列化来自同一客户端的调用.
 * 如果没有PortletSession, 则无效.
 * </td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>表示在此请求生成的渲染响应中允许指定缓存的秒数.
 * 0 (零) 表示根本不允许缓存, -1 (默认值) 不会覆盖portlet配置, 任何正数都会导致渲染响应声明缓存内容指定的秒数</td>
 * </tr>
 * <tr>
 * <td>renderWhenMinimized</td>
 * <td>false</td>
 * <td>在portlet处于最小化状态时是否应该渲染 -- 如果为false, 当portlet最小化时将为ModelandView返回null</td>
 * </tr>
 * </table>
 *
 * <p><b>TIP:</b> 控制器映射将由PortletDispatcher运行两次以执行操作请求 -- 一次用于操作阶段, 另一次用于渲染阶段.
 * 只需在控制器的操作阶段更改映射使用的条件的值(例如portlet模式或请求参数), 即可到达不同控制器的渲染阶段.
 * 这非常方便, 因为在portlet中重定向显然是不可能的.
 * 在执行此操作之前, 通常明智的做法是调用{@code clearAllRenderParameters}, 然后显式设置希望新控制器看到的所有参数.
 * 这可以避免意外的参数传递到第二个控制器的渲染阶段, 例如指示{@code AbstractFormController}中出现的表单提交的参数.
 *
 * <p>Thanks to Rainer Schmitz and Nick Lothian for their suggestions!
 */
public abstract class AbstractController extends PortletContentGenerator implements Controller {

	private boolean synchronizeOnSession = false;

	private boolean renderWhenMinimized = false;


	/**
	 * 设置是否应在会话上同步控制器执行, 以序列化来自同一客户端的并行调用.
	 * <p>更具体地说, 如果此标志为"true", 则{@code handleActionRequestInternal}方法的执行将同步.
	 * 最佳可用会话互斥锁将用于同步; 理想情况下, 这将是HttpSessionMutexListener公开的互斥锁.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作锁定当前会话的同步的安全引用.
	 * <p>在许多情况下, PortletSession引用本身也是一个安全的互斥锁, 因为它始终是同一个活动逻辑会话的相同对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一 100% 安全的方式是会话互斥.
	 */
	public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}

	/**
	 * 返回是否应在会话上同步控制器执行.
	 */
	public final boolean isSynchronizeOnSession() {
		return this.synchronizeOnSession;
	}

	/**
	 * 设置控制器是否应在portlet处于最小化窗口时渲染视图. 默认为false.
	 */
	public final void setRenderWhenMinimized(boolean renderWhenMinimized) {
		this.renderWhenMinimized = renderWhenMinimized;
	}

	/**
	 * 返回当portlet最小化时控制器是否渲染.
	 */
	public final boolean isRenderWhenMinimized() {
		return this.renderWhenMinimized;
	}


	@Override
	public void handleActionRequest(ActionRequest request, ActionResponse response) throws Exception {
		// 委派给PortletContentGenerator进行检查和准备.
		check(request, response);

		// 如果需要, 在synchronized块中执行.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					handleActionRequestInternal(request, response);
					return;
				}
			}
		}

		handleActionRequestInternal(request, response);
	}

	@Override
	public ModelAndView handleRenderRequest(RenderRequest request, RenderResponse response) throws Exception {
		// 如果portlet最小化, 不想渲染, 则返回null.
		if (WindowState.MINIMIZED.equals(request.getWindowState()) && !this.renderWhenMinimized) {
			return null;
		}

		// 委派给PortletContentGenerator进行检查和准备.
		checkAndPrepare(request, response);

		// 如果需要, 在synchronized块中执行.
		if (this.synchronizeOnSession) {
			PortletSession session = request.getPortletSession(false);
			if (session != null) {
				Object mutex = PortletUtils.getSessionMutex(session);
				synchronized (mutex) {
					return handleRenderRequestInternal(request, response);
				}
			}
		}

		return handleRenderRequestInternal(request, response);
	}


	/**
	 * 如果希望控制器处理操作请求, 则意味着子类重写此方法.
	 * 约定与{@code handleActionRequest}的约定相同.
	 * <p>默认实现抛出PortletException.
	 */
	protected void handleActionRequestInternal(ActionRequest request, ActionResponse response)
			throws Exception {

		throw new PortletException("[" + getClass().getName() + "] does not handle action requests");
	}

	/**
	 * 如果希望控制器处理渲染请求, 则意味着子类重写此方法.
	 * 约定与{@code handleRenderRequest}的约定相同.
	 * <p>默认实现抛出PortletException.
	 */
	protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response)
			throws Exception {

		throw new PortletException("[" + getClass().getName() + "] does not handle render requests");
	}
}
