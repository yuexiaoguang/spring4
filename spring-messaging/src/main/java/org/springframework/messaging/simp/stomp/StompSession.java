package org.springframework.messaging.simp.stomp;

/**
 * 表示STOMP会话, 其中包含在这些订阅上发送消息, 创建订阅和接收消息的操作.
 */
public interface StompSession {

	/**
	 * 返回会话的ID.
	 */
	String getSessionId();

	/**
	 * 会话是否已连接.
	 */
	boolean isConnected();

	/**
	 * 启用后, 会在此会话中自动将接收header添加到将来的{@code send}和{@code subscribe}操作,
	 * 这会导致服务器返回RECEIPT.
	 * 然后, 应用程序可以使用从操作返回的{@link StompSession.Receiptable Receiptable}来跟踪接收.
	 * <p>也可以通过接受{@code StompHeaders}的重载方法手动添加接收header.
	 */
	void setAutoReceipt(boolean enabled);

	/**
	 * 发送消息到指定目标, 借助{@link org.springframework.messaging.converter.MessageConverter MessageConverter}
	 * 将有效负载转换为{@code byte[]}.
	 * 
	 * @param destination 消息的目标
	 * @param payload 消息有效负载
	 * 
	 * @return 用于跟踪回执的 Receiptable
	 */
	Receiptable send(String destination, Object payload);

	/**
	 * {@link #send(String, Object)}的重载版本, 它接受完整的{@link StompHeaders}而不是目标.
	 * header必须包含目标, 并且还可能具有其他header, 例如"content-type"或代理的自定义header,
	 * 以便传播给订阅者, 或者特定于代理的非标准header.
	 * 
	 * @param headers 消息header
	 * @param payload 消息有效负载
	 * 
	 * @return 用于跟踪回执的 Receiptable
	 */
	Receiptable send(StompHeaders headers, Object payload);

	/**
	 * 通过发送SUBSCRIBE帧订阅给定目标, 并使用指定的{@link StompFrameHandler}处理收到的消息.
	 * 
	 * @param destination 要订阅的目标
	 * @param handler 收到的消息的处理器
	 * 
	 * @return 用于取消订阅和/或跟踪接收的句柄
	 */
	Subscription subscribe(String destination, StompFrameHandler handler);

	/**
	 * {@link #subscribe(String, StompFrameHandler)}的重载版本, 它接受完整的{@link StompHeaders}而不是目标.
	 * 
	 * @param headers 订阅消息帧的header
	 * @param handler 收到的消息的处理器
	 * 
	 * @return 用于取消订阅和/或跟踪接收的句柄
	 */
	Subscription subscribe(StompHeaders headers, StompFrameHandler handler);

	/**
	 * 发送确认, 确定消息被消耗, 还是分别导致ACK或NACK帧.
	 * <p><strong>Note:</strong> 要在订阅时使用此功能,
	 * 必须将{@link StompHeaders#setAck(String) ack} header设置为"client"或"client-individual", 以便使用此功能.
	 * 
	 * @param messageId 消息的id
	 * @param consumed 消息是否被消费
	 * 
	 * @return 跟踪接收的Receiptable
	 */
	Receiptable acknowledge(String messageId, boolean consumed);

	/**
	 * 通过发送DISCONNECT帧断开会话连接.
	 */
	void disconnect();


	/**
	 * 用于跟踪回执的句柄.
	 */
	interface Receiptable {

		/**
		 * 如果返回句柄的STOMP帧没有"receipt" header, 则返回接收ID或{@code null}.
		 */
		String getReceiptId();

		/**
		 * 收到回执时调用的任务.
		 * 
		 * @throws java.lang.IllegalArgumentException 如果receiptId是 {@code null}
		 */
		void addReceiptTask(Runnable runnable);

		/**
		 * 在配置的时间内未收到回执时调用的任务.
		 * 
		 * @throws java.lang.IllegalArgumentException 如果receiptId是 {@code null}
		 */
		void addReceiptLostTask(Runnable runnable);
	}


	/**
	 * 用于取消订阅或跟踪回执的句柄.
	 */
	interface Subscription extends Receiptable {

		/**
		 * 返回订阅的ID.
		 */
		String getSubscriptionId();

		/**
		 * 通过发送UNSUBSCRIBE帧删除订阅.
		 */
		void unsubscribe();
	}

}
