package org.springframework.jms.listener;

import javax.jms.Session;

import org.springframework.jms.connection.JmsResourceHolder;

/**
 * JmsResourceHolder标记子类, 指示本地公开, i.e. 不指示外部管理的事务.
 */
class LocallyExposedJmsResourceHolder extends JmsResourceHolder {

	public LocallyExposedJmsResourceHolder(Session session) {
		super(session);
	}

}
