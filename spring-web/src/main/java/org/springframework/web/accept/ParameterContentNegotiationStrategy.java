package org.springframework.web.accept;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * {@code ContentNegotiationStrategy}, 它将查询参数解析为用于查找媒体类型的键.
 * 默认参数名称是{@code format}.
 */
public class ParameterContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

	private static final Log logger = LogFactory.getLog(ParameterContentNegotiationStrategy.class);

	private String parameterName = "format";


	public ParameterContentNegotiationStrategy(Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * 设置要用于确定请求的媒体类型的参数的名称.
	 * <p>默认{@code "format"}.
	 */
	public void setParameterName(String parameterName) {
		Assert.notNull(parameterName, "'parameterName' is required");
		this.parameterName = parameterName;
	}

	public String getParameterName() {
		return this.parameterName;
	}


	@Override
	protected String getMediaTypeKey(NativeWebRequest request) {
		return request.getParameter(getParameterName());
	}

	@Override
	protected void handleMatch(String mediaTypeKey, MediaType mediaType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Requested media type: '" + mediaType + "' based on '" +
					getParameterName() + "'='" + mediaTypeKey + "'");
		}
	}

	@Override
	protected MediaType handleNoMatch(NativeWebRequest request, String key)
			throws HttpMediaTypeNotAcceptableException {

		throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
	}

}
