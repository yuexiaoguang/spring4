package org.springframework.web.servlet.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.WebContentGenerator;
import org.springframework.web.util.WebUtils;

/**
 * 控制器实现的便捷超类, 使用模板方法设计模式.
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="Controller.html#workflow">以及接口定义的那些</a>):</b><br>
 * <ol>
 * <li>{@link #handleRequest(HttpServletRequest, HttpServletResponse) handleRequest()}将由DispatcherServlet调用</li>
 * <li>检查支持的方法 (如果请求方法不支持, 则为ServletException)</li>
 * <li>如果需要session, 尝试获取它 (如果找不到抛出ServletException)</li>
 * <li>根据cacheSeconds属性, 需要时设置缓存header</li>
 * <li>调用抽象方法{@link #handleRequestInternal(HttpServletRequest, HttpServletResponse) handleRequestInternal()}
 * (可选地围绕HttpSession上的调用进行同步),
 * 应该通过扩展类来实现, 以提供返回{@link org.springframework.web.servlet.ModelAndView ModelAndView}对象的实际功能.</li>
 * </ol>
 *
 * <p><b><a name="config">暴露的配置属性</a>
 * (<a href="Controller.html#config">以及接口定义的那些</a>):</b><br>
 * <table border="1">
 * <tr>
 * <td><b>name</b></th>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>supportedMethods</td>
 * <td>GET,POST</td>
 * <td>此控制器支持的逗号分隔的 (CSV) 方法列表, 例如 GET, POST 和 PUT</td>
 * </tr>
 * <tr>
 * <td>requireSession</td>
 * <td>false</td>
 * <td>是否应该需要会话以使该控制器能够处理请求.
 * 这可以确保派生的控制器可以 - 无需担心空指针 - 调用 request.getSession() 来检索会话.
 * 如果在处理请求时找不到会话, 则抛出ServletException</td>
 * </tr>
 * <tr>
 * <td>cacheSeconds</td>
 * <td>-1</td>
 * <td>表示此请求之后的响应的缓存header中包含的秒数.
 * 0 (zero) 将包括根本不缓存的header, -1 (默认) 不会生成<i>任何headers</i>, 任何正数将生成header, 表示缓存内容的秒数</td>
 * </tr>
 * <tr>
 * <td>synchronizeOnSession</td>
 * <td>false</td>
 * <td>是否应该围绕HttpSession同步对{@code handleRequestInternal}的调用, 以序列化来自同一客户端的调用.
 * 如果没有HttpSession, 则无效.
 * </td>
 * </tr>
 * </table>
 */
public abstract class AbstractController extends WebContentGenerator implements Controller {

	private boolean synchronizeOnSession = false;


	/**
	 * 默认支持HTTP方法GET, HEAD和POST.
	 */
	public AbstractController() {
		this(true);
	}

	/**
	 * @param restrictDefaultSupportedMethods {@code true} 如果此控制器默认支持HTTP方法GET, HEAD 和 POST,
	 * 或{@code false} 如果它应该是不受限制的
	 */
	public AbstractController(boolean restrictDefaultSupportedMethods) {
		super(restrictDefaultSupportedMethods);
	}


	/**
	 * 设置是否应在会话上同步控制器执行, 以序列化来自同一客户端的并行调用.
	 * <p>更具体地说, 如果此标志为"true", 则{@code handleRequestInternal}方法的执行将同步.
	 * 最佳可用会话互斥锁将用于同步; 理想情况下, 这将是HttpSessionMutexListener公开的互斥锁.
	 * <p>会话互斥锁在会话的整个生命周期内保证是同一个对象, 在{@code SESSION_MUTEX_ATTRIBUTE}常量定义的键下可用.
	 * 它用作同步锁定当前会话的安全引用.
	 * <p>在许多情况下, HttpSession引用本身也是一个安全的互斥锁, 因为它对于同一个活动的逻辑会话始终是相同的对象引用.
	 * 但是, 不能在不同的servlet容器中保证这一点; 唯一100% 安全的方式是会话互斥.
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


	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		if (HttpMethod.OPTIONS.matches(request.getMethod())) {
			response.setHeader("Allow", getAllowHeader());
			return null;
		}

		// 委托给WebContentGenerator进行检查和准备.
		checkRequest(request);
		prepareResponse(response);

		// 如果需要, 在synchronized块中执行handleRequestInternal.
		if (this.synchronizeOnSession) {
			HttpSession session = request.getSession(false);
			if (session != null) {
				Object mutex = WebUtils.getSessionMutex(session);
				synchronized (mutex) {
					return handleRequestInternal(request, response);
				}
			}
		}

		return handleRequestInternal(request, response);
	}

	/**
	 * 模板方法. 子类必须实现这一点.
	 * 与{@code handleRequest}的约定相同.
	 */
	protected abstract ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
