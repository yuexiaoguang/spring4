package org.springframework.web.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.Assert;

/**
 * Servlet过滤器, 允许用户为请求指定字符编码.
 * 这很有用, 因为即使在HTML页面或表单中指定, 当前浏览器通常也不会设置字符编码.
 *
 * <p>如果请求尚未指定编码, 则此过滤器可以应用其编码, 或者在任何情况下强制此过滤器的编码 ("forceEncoding"="true").
 * 在后一种情况下, 编码也将作为默认响应编码应用 (尽管这通常会被视图中设置的完整内容类型覆盖).
 */
public class CharacterEncodingFilter extends OncePerRequestFilter {

	private String encoding;

	private boolean forceRequestEncoding = false;

	private boolean forceResponseEncoding = false;


	/**
	 * 通过{@link #setEncoding}设置编码.
	 */
	public CharacterEncodingFilter() {
	}

	/**
	 * @param encoding 要应用的编码
	 */
	public CharacterEncodingFilter(String encoding) {
		this(encoding, false);
	}

	/**
	 * @param encoding 要应用的编码
	 * @param forceEncoding 指定的编码是否应该覆盖现有的请求和响应编码
	 */
	public CharacterEncodingFilter(String encoding, boolean forceEncoding) {
		this(encoding, forceEncoding, forceEncoding);
	}

	/**
	 * @param encoding 要应用的编码
	 * @param forceRequestEncoding 指定的编码是否应该覆盖现有的请求编码
	 * @param forceResponseEncoding 指定的编码是否应该覆盖现有的响应编码
	 */
	public CharacterEncodingFilter(String encoding, boolean forceRequestEncoding, boolean forceResponseEncoding) {
		Assert.hasLength(encoding, "Encoding must not be empty");
		this.encoding = encoding;
		this.forceRequestEncoding = forceRequestEncoding;
		this.forceResponseEncoding = forceResponseEncoding;
	}


	/**
	 * 设置用于请求的编码.
	 * 此编码将传递到{@link javax.servlet.http.HttpServletRequest#setCharacterEncoding}调用.
	 * <p>此编码是否将覆盖现有的请求编码 (以及它是否也将作为默认响应编码应用),
	 * 取决于{@link #setForceEncoding "forceEncoding"}标志.
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回已配置的请求和/或响应的编码.
	 */
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此过滤器配置的{@link #setEncoding encoding}是否应覆盖现有请求和响应编码.
	 * <p>默认"false", i.e. 如果
	 * {@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()}返回非null值, 则不修改编码.
	 * 切换为"true"以在任何情况下强制指定的编码, 同时将其应用为默认响应编码.
	 * <p>相当于同时设置{@link #setForceRequestEncoding(boolean)}和{@link #setForceResponseEncoding(boolean)}.
	 */
	public void setForceEncoding(boolean forceEncoding) {
		this.forceRequestEncoding = forceEncoding;
		this.forceResponseEncoding = forceEncoding;
	}

	/**
	 * 设置此过滤器配置的{@link #setEncoding encoding}是否应覆盖现有请求编码.
	 * <p>默认"false", i.e. 如果
	 * {@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()} 返回非null值, 则不修改编码.
	 * 切换为"true"以在任何情况下强制指定的编码.
	 */
	public void setForceRequestEncoding(boolean forceRequestEncoding) {
		this.forceRequestEncoding = forceRequestEncoding;
	}

	/**
	 * 返回是否应对请求强制编码
	 */
	public boolean isForceRequestEncoding() {
		return this.forceRequestEncoding;
	}

	/**
	 * 设置此过滤器配置的{@link #setEncoding encoding}是否应覆盖现有响应编码.
	 * <p>默认"false", i.e. 不修改编码.
	 * 切换为"true"以在任何情况下强制指定的响应编码.
	 */
	public void setForceResponseEncoding(boolean forceResponseEncoding) {
		this.forceResponseEncoding = forceResponseEncoding;
	}

	/**
	 * 返回是否应该强制响应编码.
	 */
	public boolean isForceResponseEncoding() {
		return this.forceResponseEncoding;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String encoding = getEncoding();
		if (encoding != null) {
			if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
				request.setCharacterEncoding(encoding);
			}
			if (isForceResponseEncoding()) {
				response.setCharacterEncoding(encoding);
			}
		}
		filterChain.doFilter(request, response);
	}

}
