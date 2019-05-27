package org.springframework.scheduling.concurrent;

import java.util.concurrent.ThreadFactory;

import org.springframework.util.CustomizableThreadCreator;

/**
 * {@link java.util.concurrent.ThreadFactory}接口的实现, 允许自定义创建的线程 (名称, 优先级, etc).
 *
 * <p>有关可用配置选项的详细信息, 请参阅基类{@link org.springframework.util.CustomizableThreadCreator}.
 */
@SuppressWarnings("serial")
public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {

	/**
	 * 使用默认的线程名称前缀.
	 */
	public CustomizableThreadFactory() {
		super();
	}

	/**
	 * @param threadNamePrefix 用于新创建的线程名称的前缀
	 */
	public CustomizableThreadFactory(String threadNamePrefix) {
		super(threadNamePrefix);
	}


	@Override
	public Thread newThread(Runnable runnable) {
		return createThread(runnable);
	}

}
