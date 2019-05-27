package org.springframework.messaging.support;

import java.util.List;

/**
 * 一个{@link org.springframework.messaging.MessageChannel MessageChannel},
 * 它维护一个{@link org.springframework.messaging.support.ChannelInterceptor ChannelInterceptors}列表, 并允许拦截消息发送.
 */
public interface InterceptableChannel {

	/**
	 * 设置清除任何现有拦截器的通道拦截器列表.
	 */
	void setInterceptors(List<ChannelInterceptor> interceptors);

	/**
	 * 将通道拦截器添加到列表的末尾.
	 */
	void addInterceptor(ChannelInterceptor interceptor);

	/**
	 * 在指定的索引处添加通道拦截器.
	 */
	void addInterceptor(int index, ChannelInterceptor interceptor);

	/**
	 * 返回已配置的拦截器列表.
	 */
	List<ChannelInterceptor> getInterceptors();

	/**
	 * 删除给定的拦截器.
	 */
	boolean removeInterceptor(ChannelInterceptor interceptor);

	/**
	 * 删除给定索引处的拦截器.
	 */
	ChannelInterceptor removeInterceptor(int index);

}
