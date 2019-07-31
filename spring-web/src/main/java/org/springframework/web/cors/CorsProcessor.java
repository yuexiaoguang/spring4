package org.springframework.web.cors;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 使用请求和{@link CorsConfiguration}并更新响应的策略.
 *
 * <p>该组件不关心如何选择{@code CorsConfiguration}, 而是采取后续操作,
 * 例如应用CORS验证检查以拒绝响应或将CORS header添加到响应中.
 */
public interface CorsProcessor {

	/**
	 * 处理{@code CorsConfiguration}的请求.
	 * 
	 * @param configuration 适用的CORS配置 (possibly {@code null})
	 * @param request 当前的请求
	 * @param response 当前的响应
	 * 
	 * @return {@code false} 如果请求被拒绝, 否则{@code true}
	 */
	boolean processRequest(CorsConfiguration configuration, HttpServletRequest request,
			HttpServletResponse response) throws IOException;

}
