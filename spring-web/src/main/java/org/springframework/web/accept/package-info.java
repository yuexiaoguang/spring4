/**
 * 此包包含用于确定请求中请求的媒体类型的类.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationStrategy}是确定所请求的
 * {@linkplain org.springframework.http.MediaType 媒体类型}的主要抽象,
 * 其实现基于
 * {@linkplain org.springframework.web.accept.PathExtensionContentNegotiationStrategy 路径扩展名},
 * {@linkplain org.springframework.web.accept.ParameterContentNegotiationStrategy 请求参数},
 * {@linkplain org.springframework.web.accept.HeaderContentNegotiationStrategy 'Accept' header},
 * 或{@linkplain org.springframework.web.accept.FixedContentNegotiationStrategy 默认内容类型}.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationManager}用于按特定顺序委托给上述一种或多种策略.
 */
package org.springframework.web.accept;
