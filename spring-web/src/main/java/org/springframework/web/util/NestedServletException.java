package org.springframework.web.util;

import javax.servlet.ServletException;

import org.springframework.core.NestedExceptionUtils;

/**
 * 正如消息和堆栈跟踪一样正确处理根本原因的{@link ServletException}的子类,
 * 就像 NestedChecked/RuntimeException一样.
 *
 * <p>请注意, 普通的ServletException根本不会公开其根本原因, 无论是在异常消息中还是在打印的堆栈跟踪中!
 * 虽然这可能会在以后的Servlet API变体中修复 (对于相同的API版本, 每个供应商甚至会有所不同),
 * 但它在Servlet 2.4 (Spring 3.x所需的最低版本)上并不可靠, 这就是我们需要做的事情.
 *
 * <p>此类与NestedChecked/RuntimeException类之间的相似性是不可避免的, 因为此类需要从ServletException派生.
 */
public class NestedServletException extends ServletException {

	/** Use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -5292377985529381145L;

	static {
		// 在调用getMessage()时, 实时加载NestedExceptionUtils类以避免OSGi上的类加载器死锁问题. Reported by Don Brown; SPR-5607.
		NestedExceptionUtils.class.getName();
	}


	public NestedServletException(String msg) {
		super(msg);
	}

	public NestedServletException(String msg, Throwable cause) {
		super(msg, cause);
		// 如果不是由ServletException类完成, 则设置JDK 1.4异常链原因 (这在Servlet API版本之间不同).
		if (getCause() == null && cause!=null) {
			initCause(cause);
		}
	}


	/**
	 * 返回详细消息, 包括嵌套异常中的消息.
	 */
	@Override
	public String getMessage() {
		return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
	}

}
