package org.springframework.jms.support;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueBrowser;
import javax.jms.QueueRequestor;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.InvalidClientIDException;
import org.springframework.jms.InvalidDestinationException;
import org.springframework.jms.InvalidSelectorException;
import org.springframework.jms.JmsException;
import org.springframework.jms.JmsSecurityException;
import org.springframework.jms.MessageEOFException;
import org.springframework.jms.MessageFormatException;
import org.springframework.jms.MessageNotReadableException;
import org.springframework.jms.MessageNotWriteableException;
import org.springframework.jms.ResourceAllocationException;
import org.springframework.jms.TransactionInProgressException;
import org.springframework.jms.TransactionRolledBackException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.util.Assert;

/**
 * 使用JMS的通用工具方法.
 * 主要用于框架内部使用, 也适用于自定义JMS访问代码.
 */
public abstract class JmsUtils {

	private static final Log logger = LogFactory.getLog(JmsUtils.class);


	/**
	 * 关闭给定的JMS连接, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param con 要关闭的JMS连接 (may be {@code null})
	 */
	public static void closeConnection(Connection con) {
		closeConnection(con, false);
	}

	/**
	 * 关闭给定的JMS连接, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param con 要关闭的JMS连接 (may be {@code null})
	 * @param stop 是否在关闭前调用{@code stop()}
	 */
	public static void closeConnection(Connection con, boolean stop) {
		if (con != null) {
			try {
				if (stop) {
					try {
						con.stop();
					}
					finally {
						con.close();
					}
				}
				else {
					con.close();
				}
			}
			catch (javax.jms.IllegalStateException ex) {
				logger.debug("Ignoring Connection state exception - assuming already closed: " + ex);
			}
			catch (JMSException ex) {
				logger.debug("Could not close JMS Connection", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.debug("Unexpected exception on closing JMS Connection", ex);
			}
		}
	}

	/**
	 * 关闭给定的JMS会话, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param session 要关闭的JMS会话 (may be {@code null})
	 */
	public static void closeSession(Session session) {
		if (session != null) {
			try {
				session.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS Session", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JMS Session", ex);
			}
		}
	}

	/**
	 * 关闭给定的JMS MessageProducer, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param producer 要关闭的JMS MessageProducer (may be {@code null})
	 */
	public static void closeMessageProducer(MessageProducer producer) {
		if (producer != null) {
			try {
				producer.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS MessageProducer", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JMS MessageProducer", ex);
			}
		}
	}

	/**
	 * 关闭给定的JMS MessageConsumer, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param consumer 要关闭的JMS MessageConsumer (may be {@code null})
	 */
	public static void closeMessageConsumer(MessageConsumer consumer) {
		if (consumer != null) {
			// 清除中断以确保消费者成功关闭...
			// (解决行为不端的JMS提供商, 如ActiveMQ)
			boolean wasInterrupted = Thread.interrupted();
			try {
				consumer.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS MessageConsumer", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JMS MessageConsumer", ex);
			}
			finally {
				if (wasInterrupted) {
					// 像以前一样重置中断的标志.
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	/**
	 * 关闭给定的JMS QueueBrowser, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param browser 要关闭的JMS QueueBrowser (may be {@code null})
	 */
	public static void closeQueueBrowser(QueueBrowser browser) {
		if (browser != null) {
			try {
				browser.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS QueueBrowser", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JMS QueueBrowser", ex);
			}
		}
	}

	/**
	 * 关闭给定的JMS QueueRequestor, 并忽略任何抛出的异常.
	 * 这对于手动JMS代码中的典型{@code finally}块非常有用.
	 * 
	 * @param requestor 要关闭的JMS QueueRequestor (may be {@code null})
	 */
	public static void closeQueueRequestor(QueueRequestor requestor) {
		if (requestor != null) {
			try {
				requestor.close();
			}
			catch (JMSException ex) {
				logger.trace("Could not close JMS QueueRequestor", ex);
			}
			catch (Throwable ex) {
				// 不信任JMS提供者: 它可能抛出RuntimeException 或 Error.
				logger.trace("Unexpected exception on closing JMS QueueRequestor", ex);
			}
		}
	}

	/**
	 * 如果不在JTA事务中, 则提交会话.
	 * 
	 * @param session 要提交的JMS会话
	 * 
	 * @throws JMSException 如果提交失败
	 */
	public static void commitIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.commit();
		}
		catch (javax.jms.TransactionInProgressException ex) {
			// 忽略 -> 只能在JTA事务的情况下发生.
		}
		catch (javax.jms.IllegalStateException ex) {
			// 忽略 -> 只能在JTA事务的情况下发生.
		}
	}

	/**
	 * 如果不在JTA事务中, 则回滚会话.
	 * 
	 * @param session 要回滚的JMS会话
	 * 
	 * @throws JMSException 如果提交失败
	 */
	public static void rollbackIfNecessary(Session session) throws JMSException {
		Assert.notNull(session, "Session must not be null");
		try {
			session.rollback();
		}
		catch (javax.jms.TransactionInProgressException ex) {
			// 忽略 -> 只能在JTA事务的情况下发生.
		}
		catch (javax.jms.IllegalStateException ex) {
			// 忽略 -> 只能在JTA事务的情况下发生.
		}
	}

	/**
	 * 为给定的JMSException构建描述性异常消息, 并在适当时合并链接的异常消息.
	 * 
	 * @param ex 用于构建消息的JMSException
	 * 
	 * @return 描述性消息String
	 */
	public static String buildExceptionMessage(JMSException ex) {
		String message = ex.getMessage();
		Exception linkedEx = ex.getLinkedException();
		if (linkedEx != null) {
			if (message == null) {
				message = linkedEx.toString();
			}
			else {
				String linkedMessage = linkedEx.getMessage();
				if (linkedMessage != null && !message.contains(linkedMessage)) {
					message = message + "; nested exception is " + linkedEx;
				}
			}
		}
		return message;
	}

	/**
	 * 将指定的受检{@link javax.jms.JMSException JMSException}转换为
	 * Spring运行时{@link org.springframework.jms.JmsException JmsException}等效项.
	 * 
	 * @param ex 要转换的原始受检JMSException 异常
	 * 
	 * @return 包装给定异常的Spring运行时JmsException
	 */
	public static JmsException convertJmsAccessException(JMSException ex) {
		Assert.notNull(ex, "JMSException must not be null");

		if (ex instanceof javax.jms.IllegalStateException) {
			return new org.springframework.jms.IllegalStateException((javax.jms.IllegalStateException) ex);
		}
		if (ex instanceof javax.jms.InvalidClientIDException) {
			return new InvalidClientIDException((javax.jms.InvalidClientIDException) ex);
		}
		if (ex instanceof javax.jms.InvalidDestinationException) {
			return new InvalidDestinationException((javax.jms.InvalidDestinationException) ex);
		}
		if (ex instanceof javax.jms.InvalidSelectorException) {
			return new InvalidSelectorException((javax.jms.InvalidSelectorException) ex);
		}
		if (ex instanceof javax.jms.JMSSecurityException) {
			return new JmsSecurityException((javax.jms.JMSSecurityException) ex);
		}
		if (ex instanceof javax.jms.MessageEOFException) {
			return new MessageEOFException((javax.jms.MessageEOFException) ex);
		}
		if (ex instanceof javax.jms.MessageFormatException) {
			return new MessageFormatException((javax.jms.MessageFormatException) ex);
		}
		if (ex instanceof javax.jms.MessageNotReadableException) {
			return new MessageNotReadableException((javax.jms.MessageNotReadableException) ex);
		}
		if (ex instanceof javax.jms.MessageNotWriteableException) {
			return new MessageNotWriteableException((javax.jms.MessageNotWriteableException) ex);
		}
		if (ex instanceof javax.jms.ResourceAllocationException) {
			return new ResourceAllocationException((javax.jms.ResourceAllocationException) ex);
		}
		if (ex instanceof javax.jms.TransactionInProgressException) {
			return new TransactionInProgressException((javax.jms.TransactionInProgressException) ex);
		}
		if (ex instanceof javax.jms.TransactionRolledBackException) {
			return new TransactionRolledBackException((javax.jms.TransactionRolledBackException) ex);
		}

		// fallback
		return new UncategorizedJmsException(ex);
	}

}
