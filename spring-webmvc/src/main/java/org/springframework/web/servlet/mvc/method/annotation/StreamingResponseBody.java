package org.springframework.web.servlet.mvc.method.annotation;


import java.io.IOException;
import java.io.OutputStream;

/**
 * 用于异步请求处理的控制器方法返回值类型, 其中应用程序可以直接写入响应{@code OutputStream}, 而无需保留Servlet容器线程.
 *
 * <p><strong>Note:</strong> 使用此选项时, 强烈建议显式配置Spring MVC中使用的TaskExecutor以执行异步请求.
 * MVC Java配置和MVC命名空间都提供了配置异步处理的选项.
 * 如果不使用那些, 应用程序可以设置
 * {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter
 * RequestMappingHandlerAdapter}的{@code taskExecutor}属性.
 */
public interface StreamingResponseBody {

	/**
	 * 写入响应主体的回调.
	 * 
	 * @param outputStream 响应主体的流
	 * 
	 * @throws IOException 写入时发生异常
	 */
	void writeTo(OutputStream outputStream) throws IOException;

}
