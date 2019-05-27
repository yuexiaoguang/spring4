package org.springframework.jms.core;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Queue;

import org.springframework.jms.JmsException;

/**
 * 指定一组基本的JMS操作.
 *
 * <p>由{@link JmsTemplate}实现. 不经常使用但是增强可测试性的有用选项, 因为它很容易被模拟或存根.
 *
 * <p>提供镜像各种JMS API方法的{@code JmsTemplate}的 {@code send(..)}和{@code receive(..)}方法.
 * 有关这些方法的详细信息, 请参阅JMS规范和javadoc.
 *
 * <p>使用临时队列提供基本请求回复操作以收集回复.
 */
public interface JmsOperations {

	/**
	 * 在JMS会话中执行给定操作对象指定的操作.
	 * 
	 * @param action 公开会话的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 如果有问题
	 */
	<T> T execute(SessionCallback<T> action) throws JmsException;

	/**
	 * 将消息发送到默认JMS目标 (或为每个发送操作指定的目标).
	 * 回调提供对JMS会话和MessageProducer的访问, 以便执行复杂的发送操作.
	 * 
	 * @param action 公开会话/生产者对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T execute(ProducerCallback<T> action) throws JmsException;

	/**
	 * 将消息发送到JMS目标.
	 * 回调提供对JMS会话和MessageProducer的访问, 以便执行复杂的发送操作.
	 * 
	 * @param destination 将消息发送到的目标
	 * @param action 公开会话/生产者对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T execute(Destination destination, ProducerCallback<T> action) throws JmsException;

	/**
	 * 将消息发送到JMS目标.
	 * 回调提供对JMS会话和MessageProducer的访问, 以便执行复杂的发送操作.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param action 公开会话/生产者对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T execute(String destinationName, ProducerCallback<T> action) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

	/**
	 * 将消息发送到默认目标.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param messageCreator 创建一条消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void send(MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息到指定目标.
	 * MessageCreator回调创建给定Session的消息.
	 * 
	 * @param destination 将消息发送到的目标
	 * @param messageCreator 创建一条消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void send(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息到指定目标.
	 * MessageCreator回调创建给定Session的消息.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param messageCreator 创建一条消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void send(String destinationName, MessageCreator messageCreator) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

	/**
	 * 将给定对象发送到默认目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param message 要转换为消息的对象
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * 
	 * @param destination 将消息发送到的目标
	 * @param message 要转换为消息的对象
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(Destination destination, Object message) throws JmsException;

	/**
	 * 将给定对象发送到指定目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param message 要转换为消息的对象
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(String destinationName, Object message) throws JmsException;

	/**
	 * 将给定对象发送到默认目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * MessagePostProcessor回调允许在转换后修改消息.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param message 要转换为消息的对象
	 * @param postProcessor 修改消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * MessagePostProcessor回调允许在转换后修改消息.
	 * 
	 * @param destination 将消息发送到的目标
	 * @param message 要转换为消息的对象
	 * @param postProcessor 修改消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(Destination destination, Object message, MessagePostProcessor postProcessor)
		throws JmsException;

	/**
	 * 将给定对象发送到指定目标, 使用配置的MessageConverter将对象转换为JMS消息.
	 * MessagePostProcessor回调允许在转换后修改消息.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param message 要转换为消息的对象
	 * @param postProcessor 修改消息的回调
	 * 
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	void convertAndSend(String destinationName, Object message, MessagePostProcessor postProcessor)
		throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//---------------------------------------------------------------------------------------

	/**
	 * 从默认目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receive() throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destination 从中接收消息的目标
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receive(Destination destination) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receive(String destinationName) throws JmsException;

	/**
	 * 从默认目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receiveSelected(String messageSelector) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destination 从中接收消息的目标
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receiveSelected(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message receiveSelected(String destinationName, String messageSelector) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//---------------------------------------------------------------------------------------

	/**
	 * 从默认目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveAndConvert() throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destination 从中接收消息的目标
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveAndConvert(Destination destination) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveAndConvert(String destinationName) throws JmsException;

	/**
	 * 从默认目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveSelectedAndConvert(String messageSelector) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destination 从中接收消息的目标
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException;

	/**
	 * 从指定目标同步接收消息, 但只等待指定的传送时间.
	 * 使用配置的MessageConverter将消息转换为对象.
	 * <p>应谨慎使用此方法, 因为它将阻塞线程, 直到消息可用或超过超时值.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * 
	 * @return 消费者收到的消息, 或{@code null}如果超时到期
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages to and receiving the reply from a destination
	//---------------------------------------------------------------------------------------

	/**
	 * 发送请求消息并从默认目标接收回复.
	 * {@link MessageCreator}回调创建给定Session的消息.
	 * 临时队列作为此操作的一部分创建, 并在消息的{@code JMSReplyTO} header中设置.
	 * <p>这仅适用于指定的默认目标!
	 * 
	 * @param messageCreator 创建请求消息的回调
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message sendAndReceive(MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定目标接收回复.
	 * {@link MessageCreator}回调创建给定Session的消息.
	 * 临时队列作为此操作的一部分创建, 并在消息的{@code JMSReplyTO} header中设置.
	 * 
	 * @param destination 将消息发送到的目标
	 * @param messageCreator 创建请求消息的回调
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message sendAndReceive(Destination destination, MessageCreator messageCreator) throws JmsException;

	/**
	 * 发送消息并从指定目标接收回复.
	 * {@link MessageCreator}回调创建给定Session的消息.
	 * 临时队列作为此操作的一部分创建, 并在消息的{@code JMSReplyTO} header中设置.
	 * 
	 * @param destinationName 将消息发送到的目标的名称 (由DestinationResolver解析为实际目标)
	 * @param messageCreator 创建消息的回调
	 * 
	 * @return 回复, 或{@code null} 如果无法接收消息, 例如由于超时
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	Message sendAndReceive(String destinationName, MessageCreator messageCreator) throws JmsException;


	//---------------------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//---------------------------------------------------------------------------------------

	/**
	 * 浏览默认JMS队列中的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browse(BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览JMS队列中的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param queue 要浏览的队列
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览JMS队列中的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param queueName 要浏览的队列的名称 (由DestinationResolver解析为实际目标)
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browse(String queueName, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览JMS队列中选定的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览JMS队列中选定的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param queue 要浏览的队列
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browseSelected(Queue queue, String messageSelector, BrowserCallback<T> action) throws JmsException;

	/**
	 * 浏览JMS队列中选定的消息.
	 * 回调提供对JMS会话和QueueBrowser的访问, 以便浏览队列并对内容作出反应.
	 * 
	 * @param queueName 要浏览的队列的名称 (由DestinationResolver解析为实际目标)
	 * @param messageSelector JMS消息选择器表达式 (或{@code null}).
	 * 有关选择器表达式的详细定义, 请参阅JMS规范.
	 * @param action 公开会话/浏览器对的回调对象
	 * 
	 * @return 使用会话的结果对象
	 * @throws JmsException 受检的JMSException异常已转换为非受检的异常
	 */
	<T> T browseSelected(String queueName, String messageSelector, BrowserCallback<T> action) throws JmsException;

}
