package org.springframework.beans.factory.parsing;

import java.util.EventListener;

/**
 * 在bean定义读取过程中接收组件、别名、导入注册的回调的接口.
 */
public interface ReaderEventListener extends EventListener {

	/**
	 * 已注册给定的默认值的通知.
	 * 
	 * @param defaultsDefinition 默认值的描述符
	 */
	void defaultsRegistered(DefaultsDefinition defaultsDefinition);

	/**
	 * 给定组件已注册的通知.
	 * 
	 * @param componentDefinition 新组件的描述符
	 */
	void componentRegistered(ComponentDefinition componentDefinition);

	/**
	 * 给定别名已注册的通知.
	 * 
	 * @param aliasDefinition 新别名的描述符
	 */
	void aliasRegistered(AliasDefinition aliasDefinition);

	/**
	 * 已处理给定导入的通知.
	 * 
	 * @param importDefinition 导入的描述符
	 */
	void importProcessed(ImportDefinition importDefinition);

}
