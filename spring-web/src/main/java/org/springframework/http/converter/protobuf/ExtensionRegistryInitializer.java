package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

/**
 * 如果在{@code ExtensionRegistry}中注册了相应的配置, Google协议消息可以包含可解析的消息扩展.
 *
 * <p>此接口提供了填充{@code ExtensionRegistry}的工具.
 */
public interface ExtensionRegistryInitializer {

	/**
	 * 使用协议消息扩展初始化{@code ExtensionRegistry}.
	 * 
	 * @param registry 要填充的注册表
	 */
    void initializeExtensionRegistry(ExtensionRegistry registry);

}
