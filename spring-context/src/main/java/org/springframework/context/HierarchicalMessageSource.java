package org.springframework.context;

/**
 * MessageSource的子接口由可以分层次地解析消息的对象实现.
 */
public interface HierarchicalMessageSource extends MessageSource {

	/**
	 * 设置将用于尝试解析此对象无法解析的消息的父级.
	 * 
	 * @param parent 将用于解析此对象无法解析的消息的父级MessageSource.
	 * 可能是{@code null}, 在这种情况下无法进一步解析.
	 */
	void setParentMessageSource(MessageSource parent);

	/**
	 * 返回此MessageSource的父级, 如果没有, 则返回{@code null}.
	 */
	MessageSource getParentMessageSource();

}
