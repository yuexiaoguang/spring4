package org.springframework.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于使用{@link UriComponentsBuilder}进行扩展和编码变量的{@link UriTemplateHandler}的默认实现.
 *
 * <p>还有几个属性可以自定义URI模板处理的执行方式,
 * 包括{@link #setBaseUrl baseUrl}用作所有URI模板的前缀和一些编码相关选项 &mdash;
 * {@link #setParsePath parsePath}和{@link #setStrictEncoding strictEncoding}.
 */
public class DefaultUriTemplateHandler extends AbstractUriTemplateHandler {

	private boolean parsePath;

	private boolean strictEncoding;


	/**
	 * 是否将URI模板字符串的路径解析为路径段.
	 * <p>如果设置为{@code true}, 则URI模板路径会立即分解为路径段,
	 * 因此扩展到其中的任何URI变量都受路径段编码规则的约束.
	 * 实际上, 路径中的URI变量具有百分号编码的 "/"字符.
	 * <p>默认为{@code false}, 在这种情况下, 路径保持为完整路径, 扩展的URI变量将保留 "/" 字符.
	 * 
	 * @param parsePath 是否将路径解析为路径段
	 */
	public void setParsePath(boolean parsePath) {
		this.parsePath = parsePath;
	}

	/**
	 * 处理器是否配置为将路径解析为路径段.
	 */
	public boolean shouldParsePath() {
		return this.parsePath;
	}

	/**
	 * 是否编码<a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 Section 2</a>中定义的非保留集之外的字符.
	 * 这可确保URI变量值不包含任何具有保留目的的字符.
	 * <p>默认为{@code false}, 在这种情况下, 只编码给定URI组件非法的字符.
	 * 例如, 当将URI变量扩展为路径段时, "/" 字符是非法的并且是编码的.
	 * 然而, 即使 ";" 字符具有保留的目的, 它也是合法的而不是编码的.
	 * <p><strong>Note:</strong> 此属性不需要设置{@link #setParsePath parsePath}属性.
	 * 
	 * @param strictEncoding 是否执行严格的编码
	 */
	public void setStrictEncoding(boolean strictEncoding) {
		this.strictEncoding = strictEncoding;
	}

	/**
	 * 是否严格编码未保留集之外的任何字符.
	 */
	public boolean isStrictEncoding() {
		return this.strictEncoding;
	}


	@Override
	protected URI expandInternal(String uriTemplate, Map<String, ?> uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	@Override
	protected URI expandInternal(String uriTemplate, Object... uriVariables) {
		UriComponentsBuilder uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
		UriComponents uriComponents = expandAndEncode(uriComponentsBuilder, uriVariables);
		return createUri(uriComponents);
	}

	/**
	 * 从URI模板字符串创建{@code UriComponentsBuilder}.
	 * 此实现还会根据是否启用了{@link #setParsePath parsePath}将路径分解为路径段.
	 */
	protected UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
		if (shouldParsePath() && !isStrictEncoding()) {
			List<String> pathSegments = builder.build().getPathSegments();
			builder.replacePath(null);
			for (String pathSegment : pathSegments) {
				builder.pathSegment(pathSegment);
			}
		}
		return builder;
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Map<String, ?> uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Map<String, Object> encodedUriVars = new HashMap<String, Object>(uriVariables.size());
			for (Map.Entry<String, ?> entry : uriVariables.entrySet()) {
				encodedUriVars.put(entry.getKey(), applyStrictEncoding(entry.getValue()));
			}
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	protected UriComponents expandAndEncode(UriComponentsBuilder builder, Object[] uriVariables) {
		if (!isStrictEncoding()) {
			return builder.buildAndExpand(uriVariables).encode();
		}
		else {
			Object[] encodedUriVars = new Object[uriVariables.length];
			for (int i = 0; i < uriVariables.length; i++) {
				encodedUriVars[i] = applyStrictEncoding(uriVariables[i]);
			}
			return builder.buildAndExpand(encodedUriVars);
		}
	}

	private String applyStrictEncoding(Object value) {
		String stringValue = (value != null ? value.toString() : "");
		try {
			return UriUtils.encode(stringValue, "UTF-8");
		}
		catch (UnsupportedEncodingException ex) {
			// Should never happen
			throw new IllegalStateException("Failed to encode URI variable", ex);
		}
	}

	private URI createUri(UriComponents uriComponents) {
		try {
			// 避免进一步编码 (在 strictEncoding=true 的情况下)
			return new URI(uriComponents.toUriString());
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Could not create URI object: " + ex.getMessage(), ex);
		}
	}

}
