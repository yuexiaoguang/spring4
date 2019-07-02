package org.springframework.test.web.client.response;

import java.net.URI;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ResponseCreator;

/**
 * 用于获取{@link ResponseCreator}实例的静态工厂方法.
 *
 * <p><strong>Eclipse users:</strong> 考虑将此类添加为Java编辑器的最爱. 要导航，请打开“首选项”并键入"favorites".
 */
public abstract class MockRestResponseCreators {

	/**
	 * 获得200响应 (OK)的{@code ResponseCreator}.
	 */
	public static DefaultResponseCreator withSuccess() {
		return new DefaultResponseCreator(HttpStatus.OK);
	}

	/**
	 * 使用String 主体的200响应 (OK)的{@code ResponseCreator}.
	 * 
	 * @param body 响应主体, "UTF-8"字符串
	 * @param mediaType 内容的类型, 可能是{@code null}
	 */
	public static DefaultResponseCreator withSuccess(String body, MediaType mediaType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(mediaType);
	}

	/**
	 * 使用byte[] 主体的200响应 (OK)的{@code ResponseCreator}.
	 * 
	 * @param body 响应主体
	 * @param contentType 内容的类型, 可能是{@code null}
	 */
	public static DefaultResponseCreator withSuccess(byte[] body, MediaType contentType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(contentType);
	}

	/**
	 * 基于{@link Resource}的正文的内容的200响应 (OK).
	 * 
	 * @param body 响应主体
	 * @param contentType 内容的类型, 可能是{@code null}
	 */
	public static DefaultResponseCreator withSuccess(Resource body, MediaType contentType) {
		return new DefaultResponseCreator(HttpStatus.OK).body(body).contentType(contentType);
	}

	/**
	 * 带有'Location' header的201响应(CREATED).
	 * 
	 * @param location {@code Location} header的值
	 */
	public static DefaultResponseCreator withCreatedEntity(URI location) {
		return new DefaultResponseCreator(HttpStatus.CREATED).location(location);
	}

	/**
	 * 204响应 (NO_CONTENT).
	 */
	public static DefaultResponseCreator withNoContent() {
		return new DefaultResponseCreator(HttpStatus.NO_CONTENT);
	}

	/**
	 * 400响应 (BAD_REQUEST).
	 */
	public static DefaultResponseCreator withBadRequest() {
		return new DefaultResponseCreator(HttpStatus.BAD_REQUEST);
	}

	/**
	 * 401响应 (UNAUTHORIZED).
	 */
	public static DefaultResponseCreator withUnauthorizedRequest() {
		return new DefaultResponseCreator(HttpStatus.UNAUTHORIZED);
	}

	/**
	 * 500响应 (SERVER_ERROR).
	 */
	public static DefaultResponseCreator withServerError() {
		return new DefaultResponseCreator(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * 具有指定HTTP状态的{@code ResponseCreator}.
	 * 
	 * @param status 响应状态
	 */
	public static DefaultResponseCreator withStatus(HttpStatus status) {
		return new DefaultResponseCreator(status);
	}

}
