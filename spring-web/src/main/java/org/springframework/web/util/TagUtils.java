package org.springframework.web.util;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.Assert;

/**
 * 标记库相关代码的工具类, 公开了将{@link String Strings}转换为Web范围等功能.
 *
 * <p>
 * <ul>
 * <li>{@code page}将转换为
 * {@link javax.servlet.jsp.PageContext#PAGE_SCOPE PageContext.PAGE_SCOPE}
 * <li>{@code request}将转换为
 * {@link javax.servlet.jsp.PageContext#REQUEST_SCOPE PageContext.REQUEST_SCOPE}
 * <li>{@code session}将转换为
 * {@link javax.servlet.jsp.PageContext#SESSION_SCOPE PageContext.SESSION_SCOPE}
 * <li>{@code application}将转换为
 * {@link javax.servlet.jsp.PageContext#APPLICATION_SCOPE PageContext.APPLICATION_SCOPE}
 * </ul>
 */
public abstract class TagUtils {

	/** 标识页面范围的常量 */
	public static final String SCOPE_PAGE = "page";

	/** 标识请求范围的常量 */
	public static final String SCOPE_REQUEST = "request";

	/** 标识会话范围的常量 */
	public static final String SCOPE_SESSION = "session";

	/** 识别应用程序范围的常量 */
	public static final String SCOPE_APPLICATION = "application";


	/**
	 * 确定给定输入{@code String}的范围.
	 * <p>如果{@code String}与'request', 'session', 'page' 或'application'不匹配,
	 * 则该方法将返回{{@link PageContext#PAGE_SCOPE}.
	 * 
	 * @param scope 要检查的{@code String}
	 * 
	 * @return 找到的范围, 或{@link PageContext#PAGE_SCOPE} 如果没有匹配的范围
	 * @throws IllegalArgumentException 如果提供的{@code scope}是{@code null}
	 */
	public static int getScope(String scope) {
		Assert.notNull(scope, "Scope to search for cannot be null");
		if (scope.equals(SCOPE_REQUEST)) {
			return PageContext.REQUEST_SCOPE;
		}
		else if (scope.equals(SCOPE_SESSION)) {
			return PageContext.SESSION_SCOPE;
		}
		else if (scope.equals(SCOPE_APPLICATION)) {
			return PageContext.APPLICATION_SCOPE;
		}
		else {
			return PageContext.PAGE_SCOPE;
		}
	}

	/**
	 * 确定提供的{@link Tag}是否具有所提供类型的祖先标记.
	 * 
	 * @param tag 要检查其祖先的标记
	 * @param ancestorTagClass 正在搜索的祖先{@link Class}
	 * 
	 * @return {@code true} 如果提供的{@link Tag}具有所提供类型的祖先标记
	 * @throws IllegalArgumentException 如果提供的参数中的任何一个是{@code null};
	 * 或者如果提供的{@code ancestorTagClass}不是可分配给{@link Tag}类的类型
	 */
	public static boolean hasAncestorOfType(Tag tag, Class<?> ancestorTagClass) {
		Assert.notNull(tag, "Tag cannot be null");
		Assert.notNull(ancestorTagClass, "Ancestor tag class cannot be null");
		if (!Tag.class.isAssignableFrom(ancestorTagClass)) {
			throw new IllegalArgumentException(
					"Class '" + ancestorTagClass.getName() + "' is not a valid Tag type");
		}
		Tag ancestor = tag.getParent();
		while (ancestor != null) {
			if (ancestorTagClass.isAssignableFrom(ancestor.getClass())) {
				return true;
			}
			ancestor = ancestor.getParent();
		}
		return false;
	}

	/**
	 * 确定提供的{@link Tag}是否具有所提供类型的祖先标记, 如果没有, 则抛出{@link IllegalStateException}.
	 * 
	 * @param tag 要检查其祖先的标记
	 * @param ancestorTagClass 正在搜索的祖先{@link Class}
	 * @param tagName {@code tag}的名称; 例如'{@code option}'
	 * @param ancestorTagName 祖先{@code tag}的名称; 例如'{@code select}'
	 * 
	 * @throws IllegalStateException 如果提供的{@code tag}没有提供的{@code parentTagClass}作为祖先的标记
	 * @throws IllegalArgumentException 如果任何提供的参数是{@code null}, 或者完全由空格组成的{@link String};
	 * 或者如果提供的{@code ancestorTagClass}不是可分配给{@link Tag}类的类型
	 */
	public static void assertHasAncestorOfType(Tag tag, Class<?> ancestorTagClass, String tagName, String ancestorTagName) {
		Assert.hasText(tagName, "'tagName' must not be empty");
		Assert.hasText(ancestorTagName, "'ancestorTagName' must not be empty");
		if (!TagUtils.hasAncestorOfType(tag, ancestorTagClass)) {
			throw new IllegalStateException("The '" + tagName + "' tag can only be used inside a valid '" + ancestorTagName + "' tag.");
		}
	}

}
