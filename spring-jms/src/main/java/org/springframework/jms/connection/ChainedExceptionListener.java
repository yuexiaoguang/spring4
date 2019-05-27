package org.springframework.jms.connection;

import java.util.ArrayList;
import java.util.List;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;

import org.springframework.util.Assert;

/**
 * JMS ExceptionListener接口的实现, 支持链, 允许按顺序添加多个ExceptionListener实例.
 */
public class ChainedExceptionListener implements ExceptionListener {

	/** List of ExceptionListeners */
	private final List<ExceptionListener> delegates = new ArrayList<ExceptionListener>(2);


	/**
	 * 将ExceptionListener添加到链接的委托列表中.
	 */
	public final void addDelegate(ExceptionListener listener) {
		Assert.notNull(listener, "ExceptionListener must not be null");
		this.delegates.add(listener);
	}

	/**
	 * 返回所有已注册的ExceptionListener委托.
	 */
	public final ExceptionListener[] getDelegates() {
		return this.delegates.toArray(new ExceptionListener[this.delegates.size()]);
	}


	@Override
	public void onException(JMSException ex) {
		for (ExceptionListener listener : this.delegates) {
			listener.onException(ex);
		}
	}

}
