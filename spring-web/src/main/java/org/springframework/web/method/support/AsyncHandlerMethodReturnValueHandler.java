package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;

/**
 * 支持异步类型的返回值处理器.
 * 需要优先处理此类返回值类型, 以便可以"解包"异步值.
 *
 * <p><strong>Note: </strong> 实现此约定不是必需的, 但是当处理器需要优先于其他时, 应该实现它.
 * 例如, 默认情况下, 在内置处理器之后排序的自定义 (异步)处理器应优先于 {@code @ResponseBody}或{@code @ModelAttribute}处理,
 * 这应该在异步值准备就绪后发生.
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * 给定的返回值是否表示异步计算.
	 * 
	 * @param returnValue 返回值
	 * @param returnType 返回类型
	 * 
	 * @return {@code true} 如果返回值是异步的.
	 */
	boolean isAsyncReturnValue(Object returnValue, MethodParameter returnType);

}
