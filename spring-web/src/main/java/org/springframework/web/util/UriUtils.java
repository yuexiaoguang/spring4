package org.springframework.web.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import org.springframework.util.Assert;

/**
 * 用于基于RFC 3986的URI编码和解码的工具类.
 * 提供各种URI组件的编码方法.
 *
 * <p>此类中的所有{@code encode*(String, String)}方法以类似的方式运行:
 * <ul>
 * <li>RFC 3986中定义的特定URI组件的有效字符保持不变.</li>
 * <li>在给定的编码模式中, 所有其他字符被转换为一个或多个字节.
 * 每个结果字节都以"<code>%<i>xy</i></code>"格式写成十六进制字符串.</li>
 * </ul>
 */
public abstract class UriUtils {

	/**
	 * 使用给定的编码对给定的URI scheme进行编码.
	 * 
	 * @param scheme 要编码的scheme
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的scheme
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeScheme(String scheme, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
	}

	/**
	 * 使用给定的编码对给定的URI权限进行编码.
	 * 
	 * @param authority 要编码的authority
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的authority
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeAuthority(String authority, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
	}

	/**
	 * 使用给定的编码对给定的URI用户信息进行编码.
	 * 
	 * @param userInfo 要编码的用户信息
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的用户信息
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeUserInfo(String userInfo, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
	}

	/**
	 * 使用给定的编码对给定的URI主机进行编码.
	 * 
	 * @param host 要编码的主机
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的主机
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeHost(String host, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
	}

	/**
	 * 使用给定的编码对给定的URI端口进行编码.
	 * 
	 * @param port 要编码的端口
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的端口
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodePort(String port, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(port, encoding, HierarchicalUriComponents.Type.PORT);
	}

	/**
	 * 使用给定的编码对给定的URI路径进行编码.
	 * 
	 * @param path 要编码的路径
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的路径
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodePath(String path, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(path, encoding, HierarchicalUriComponents.Type.PATH);
	}

	/**
	 * 使用给定的编码对给定的URI路径分段进行编码.
	 * 
	 * @param segment 要编码的分段
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的分段
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodePathSegment(String segment, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
	}

	/**
	 * 使用给定的编码对给定的URI查询进行编码.
	 * 
	 * @param query 要编码的查询
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的查询
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeQuery(String query, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(query, encoding, HierarchicalUriComponents.Type.QUERY);
	}

	/**
	 * 使用给定的编码对给定的URI查询参数进行编码.
	 * 
	 * @param queryParam 要编码的查询参数
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的查询参数
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeQueryParam(String queryParam, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
	}

	/**
	 * 使用给定的编码对给定的URI片段进行编码.
	 * 
	 * @param fragment 要编码的片段
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的片段
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encodeFragment(String fragment, String encoding) throws UnsupportedEncodingException {
		return HierarchicalUriComponents.encodeUriComponent(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
	}

	/**
	 * 编码<a href="https://tools.ietf.org/html/rfc3986#section-2">RFC 3986 Section 2</a>中定义的非保留字符集之外的字符.
	 * <p>这可以用于确保给定的String不包含任何具有保留URI含义的字符, 而不管URI组件如何.
	 * 
	 * @param source 要编码的字符串
	 * @param encoding 字符编码
	 * 
	 * @return 编码后的字符串
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String encode(String source, String encoding) throws UnsupportedEncodingException {
		HierarchicalUriComponents.Type type = HierarchicalUriComponents.Type.URI;
		return HierarchicalUriComponents.encodeUriComponent(source, encoding, type);
	}

	/**
	 * 解码给定的编码URI组件.
	 * <ul>
	 * <li>字母数字字符{@code "a"}到{@code "z"}, {@code "A"}到{@code "Z"}, {@code "0"}到{@code "9"}, 保持原样.</li>
	 * <li>特殊字符{@code "-"}, {@code "_"}, {@code "."}, {@code "*"}, 保持原样.</li>
	 * <li>序列"{@code %<i>xy</i>}"被解释为字符的十六进制表示.</li>
	 * </ul>
	 * 
	 * @param source 已编码的字符串
	 * @param encoding 编码
	 * 
	 * @return 解码后的值
	 * @throws IllegalArgumentException 当给定的源包含无效的编码序列时
	 * @throws UnsupportedEncodingException 不支持给定的编码参数
	 */
	public static String decode(String source, String encoding) throws UnsupportedEncodingException {
		if (source == null) {
			return null;
		}
		Assert.hasLength(encoding, "Encoding must not be empty");
		int length = source.length();
		ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if ((i + 2) < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					bos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				}
				else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			}
			else {
				bos.write(ch);
			}
		}
		return (changed ? new String(bos.toByteArray(), encoding) : source);
	}

	/**
	 * 从给定的URI路径中提取文件扩展名.
	 * 
	 * @param path URI路径 (e.g. "/products/index.html")
	 * 
	 * @return 提取的文件扩展名 (e.g. "html")
	 */
	public static String extractFileExtension(String path) {
		int end = path.indexOf('?');
		int fragmentIndex = path.indexOf('#');
		if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
			end = fragmentIndex;
		}
		if (end == -1) {
			end = path.length();
		}
		int begin = path.lastIndexOf('/', end) + 1;
		int paramIndex = path.indexOf(';', begin);
		end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
		int extIndex = path.lastIndexOf('.', end);
		if (extIndex != -1 && extIndex > begin) {
			return path.substring(extIndex + 1, end);
		}
		return null;
	}

}
