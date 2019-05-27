package org.springframework.messaging.core;

/**
 * 将String目标名称解析为{@code <D>}类型的实际目标的策略.
 */
public interface DestinationResolver<D> {

	/**
	 * 解析给定的目标名称.
	 * 
	 * @param name 要解析的目标名称
	 * 
	 * @return 解析后的目标 (never {@code null})
	 * @throws DestinationResolutionException 如果找不到指定的目标或因任何其他原因无法解析
	 */
	D resolveDestination(String name) throws DestinationResolutionException;

}
