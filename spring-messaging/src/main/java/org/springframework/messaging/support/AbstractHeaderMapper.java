package org.springframework.messaging.support;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * 基础{@link HeaderMapper}实现.
 */
public abstract class AbstractHeaderMapper<T> implements HeaderMapper<T> {

	protected final Log logger = LogFactory.getLog(getClass());

	private String inboundPrefix = "";

	private String outboundPrefix = "";


	/**
	 * 为要映射到MessageHeaders的任何用户定义属性, 指定要附加到消息header名称的前缀.
	 * 默认空字符串 (没有前缀).
	 */
	public void setInboundPrefix(String inboundPrefix) {
		this.inboundPrefix = (inboundPrefix != null ? inboundPrefix : "");
	}

	/**
	 * 为要映射到特定于协议的消息的任何用户定义的消息header, 指定要附加到协议属性名称的前缀.
	 * 默认空字符串 (没有前缀).
	 */
	public void setOutboundPrefix(String outboundPrefix) {
		this.outboundPrefix = (outboundPrefix != null ? outboundPrefix : "");
	}


	/**
	 * 生成用于将指定的{@code headerName}定义的header设置为协议特定消息的名称.
	 */
	protected String fromHeaderName(String headerName) {
		String propertyName = headerName;
		if (StringUtils.hasText(this.outboundPrefix) && !propertyName.startsWith(this.outboundPrefix)) {
			propertyName = this.outboundPrefix + headerName;
		}
		return propertyName;
	}

	/**
	 * 生成用于将指定的{@code propertyName}定义的header设置为{@link MessageHeaders}实例的名称.
	 */
	protected String toHeaderName(String propertyName) {
		String headerName = propertyName;
		if (StringUtils.hasText(this.inboundPrefix) && !headerName.startsWith(this.inboundPrefix)) {
			headerName = this.inboundPrefix + propertyName;
		}
		return headerName;
	}

	/**
	 * 返回header值, 如果它不存在或与请求的{@code type}不匹配, 则返回{@code null}.
	 */
	protected <V> V getHeaderIfAvailable(Map<String, Object> headers, String name, Class<V> type) {
		Object value = headers.get(name);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			if (logger.isWarnEnabled()) {
				logger.warn("Skipping header '" + name + "'expected type [" + type + "], but got [" +
						value.getClass() + "]");
			}
			return null;
		}
		else {
			return type.cast(value);
		}
	}
}
