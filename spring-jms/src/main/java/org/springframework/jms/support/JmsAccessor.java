package org.springframework.jms.support;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Constants;
import org.springframework.jms.JmsException;

/**
 * {@link org.springframework.jms.core.JmsTemplate}和其他JMS访问网关的基类,
 * 定义了JMS {@link ConnectionFactory}等常用属性.
 * 子类{@link org.springframework.jms.support.destination.JmsDestinationAccessor}进一步添加了与目标相关的属性.
 *
 * <p>不打算直接使用.
 * See {@link org.springframework.jms.core.JmsTemplate}.
 */
public abstract class JmsAccessor implements InitializingBean {

	/** Constants instance for javax.jms.Session */
	private static final Constants sessionConstants = new Constants(Session.class);


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private ConnectionFactory connectionFactory;

	private boolean sessionTransacted = false;

	private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;


	/**
	 * 设置用于获取JMS {@link Connection Connections}的ConnectionFactory.
	 */
	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * 返回此访问器用于获取JMS {@link Connection Connections}的ConnectionFactory.
	 */
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * 设置创建JMS {@link Session}时使用的事务模式.
	 * 默认"false".
	 * <p>请注意, 在JTA事务中, 传递给{@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}方法的参数不会被考虑在内.
	 * 根据Java EE事务上下文, 容器会自行决定这些值.
	 * 类似地, 在本地管理的事务中也不考虑这些参数, 因为在这种情况下访问器在现有的JMS会话上操作.
	 * <p>设置为"true"将在托管的事务外部运行时使用短的本地JMS事务, 并且在托管的事务(XA事务除外)的情况下使用同步的本地JMS事务.
	 * 这具有本地JMS事务与主事务 (可能是本机JDBC事务)一起管理的效果, JMS事务在主事务之后立即提交.
	 */
	public void setSessionTransacted(boolean sessionTransacted) {
		this.sessionTransacted = sessionTransacted;
	}

	/**
	 * 返回此访问器使用的JMS {@link Session sessions}是否应该被处理.
	 */
	public boolean isSessionTransacted() {
		return this.sessionTransacted;
	}

	/**
	 * 通过JMS {@link Session}接口中相应常量的名称设置JMS确认模式, e.g. "CLIENT_ACKNOWLEDGE".
	 * <p>如果要对确认模式使用特定于供应商的扩展, 使用{@link #setSessionAcknowledgeMode(int)}.
	 * 
	 * @param constantName {@link Session}确认模式常量的名称
	 */
	public void setSessionAcknowledgeModeName(String constantName) {
		setSessionAcknowledgeMode(sessionConstants.asNumber(constantName).intValue());
	}

	/**
	 * 设置创建JMS {@link Session}以发送消息时使用的JMS确认模式.
	 * <p>默认{@link Session#AUTO_ACKNOWLEDGE}.
	 * <p>也可以在此处设置确认模式的特定于供应商的扩展.
	 * <p>请注意, 在EJB内部, 不考虑{@code create(Queue/Topic)Session(boolean transacted, int acknowledgeMode)}方法的参数.
	 * 根据EJB中的事务上下文, 容器会自行决定这些值.
	 * See section 17.3.5 of the EJB spec.
	 * 
	 * @param sessionAcknowledgeMode 确认模式常量
	 */
	public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.sessionAcknowledgeMode = sessionAcknowledgeMode;
	}

	/**
	 * 返回JMS {@link Session sessions}的确认模式.
	 */
	public int getSessionAcknowledgeMode() {
		return this.sessionAcknowledgeMode;
	}

	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * 将指定的受检{@link javax.jms.JMSException JMSException}转换为
	 * Spring运行时{@link org.springframework.jms.JmsException JmsException}等效项.
	 * <p>默认实现委托给{@link JmsUtils#convertJmsAccessException}方法.
	 * 
	 * @param ex 要转换的原始受检{@link JMSException}
	 * 
	 * @return 包装{@code ex}的Spring运行时{@link JmsException}
	 */
	protected JmsException convertJmsAccessException(JMSException ex) {
		return JmsUtils.convertJmsAccessException(ex);
	}

	/**
	 * 通过此模板的ConnectionFactory创建JMS连接.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @return 新的JMS Connection
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Connection createConnection() throws JMSException {
		return getConnectionFactory().createConnection();
	}

	/**
	 * 为给定的Connection创建JMS会话.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param con 用于创建会话的JMS连接
	 * 
	 * @return 新的JMS Session
	 * @throws JMSException 如果由JMS API方法抛出
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(isSessionTransacted(), getSessionAcknowledgeMode());
	}

	/**
	 * 确定给定的会话是否处于客户端确认模式.
	 * <p>此实现使用JMS 1.1 API.
	 * 
	 * @param session 要检查的JMS会话
	 * 
	 * @return 给定的会话是否处于客户端确认模式
	 * @throws javax.jms.JMSException 如果由JMS API方法抛出
	 */
	protected boolean isClientAcknowledge(Session session) throws JMSException {
		return (session.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE);
	}
}
