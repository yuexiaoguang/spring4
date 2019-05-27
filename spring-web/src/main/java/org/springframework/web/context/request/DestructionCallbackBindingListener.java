package org.springframework.web.context.request;

import java.io.Serializable;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

/**
 * Adapter that implements the Servlet HttpSessionBindingListener interface,
 * wrapping a session destruction callback.
 */
@SuppressWarnings("serial")
public class DestructionCallbackBindingListener implements HttpSessionBindingListener, Serializable {

	private final Runnable destructionCallback;


	/**
	 * Create a new DestructionCallbackBindingListener for the given callback.
	 * @param destructionCallback the Runnable to execute when this listener
	 * object gets unbound from the session
	 */
	public DestructionCallbackBindingListener(Runnable destructionCallback) {
		this.destructionCallback = destructionCallback;
	}


	@Override
	public void valueBound(HttpSessionBindingEvent event) {
	}

	@Override
	public void valueUnbound(HttpSessionBindingEvent event) {
		this.destructionCallback.run();
	}

}
