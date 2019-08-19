package org.springframework.web.servlet.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;

/**
 * 所有需要{@link RequestContext}的标签的超类.
 *
 * <p>{@code RequestContext}实例提供了对
 * {@link org.springframework.web.context.WebApplicationContext},
 * {@link java.util.Locale}, {@link org.springframework.ui.context.Theme}等当前状态的轻松访问.
 *
 * <p>主要用于 {@link org.springframework.web.servlet.DispatcherServlet}请求;
 * 在{@code DispatcherServlet}之外使用时将使用回退.
 */
@SuppressWarnings("serial")
public abstract class RequestContextAwareTag extends TagSupport implements TryCatchFinally {

	/**
	 * 页面级{@link RequestContext}实例的{@link javax.servlet.jsp.PageContext}属性.
	 */
	public static final String REQUEST_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.REQUEST_CONTEXT";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());


	private RequestContext requestContext;


	/**
	 * 委托给{@link #doStartTagInternal()}进行实际的工作.
	 */
	@Override
	public final int doStartTag() throws JspException {
		try {
			this.requestContext = (RequestContext) this.pageContext.getAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE);
			if (this.requestContext == null) {
				this.requestContext = new JspAwareRequestContext(this.pageContext);
				this.pageContext.setAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE, this.requestContext);
			}
			return doStartTagInternal();
		}
		catch (JspException ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		catch (RuntimeException ex) {
			logger.error(ex.getMessage(), ex);
			throw ex;
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
			throw new JspTagException(ex.getMessage());
		}
	}

	/**
	 * 返回当前的RequestContext.
	 */
	protected final RequestContext getRequestContext() {
		return this.requestContext;
	}

	/**
	 * 由doStartTag调用以执行实际工作.
	 * 
	 * @return 和TagSupport.doStartTag一样
	 * @throws Exception 任何异常, 除了JspException之外的任何受检异常都会被doStartTag包装在JspException中
	 */
	protected abstract int doStartTagInternal() throws Exception;


	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.requestContext = null;
	}

}
