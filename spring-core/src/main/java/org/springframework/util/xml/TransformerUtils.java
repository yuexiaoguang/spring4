package org.springframework.util.xml;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.springframework.util.Assert;

/**
 * 包含与{@link javax.xml.transform.Transformer Transformers}和{@code javax.xml.transform}包有关的常见行为.
 */
public abstract class TransformerUtils {

	/**
	 * 如果启用{@link #enableIndenting 缩进}, 则缩进字符数量.
	 * <p>默认 "2".
	 */
	public static final int DEFAULT_INDENT_AMOUNT = 2;


	/**
	 * 为所提供的{@link javax.xml.transform.Transformer}启用缩进.
	 * <p>如果底层XSLT引擎是Xalan, 那么特殊输出键{@code indent-amount}也将被设置为{@link #DEFAULT_INDENT_AMOUNT}个字符的值.
	 * 
	 * @param transformer 目标转换器
	 */
	public static void enableIndenting(Transformer transformer) {
		enableIndenting(transformer, DEFAULT_INDENT_AMOUNT);
	}

	/**
	 * 为所提供的{@link javax.xml.transform.Transformer}启用缩进.
	 * <p>如果底层XSLT引擎是Xalan, 那么特殊输出键{@code indent-amount}也将被设置为{@link #DEFAULT_INDENT_AMOUNT}个字符的值.
	 * 
	 * @param transformer 目标转换器
	 * @param indentAmount 缩进的大小 (2个字符, 3个字符等)
	 */
	public static void enableIndenting(Transformer transformer, int indentAmount) {
		Assert.notNull(transformer, "Transformer must not be null");
		if (indentAmount < 0) {
			throw new IllegalArgumentException("Invalid indent amount (must not be less than zero): " + indentAmount);
		}
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		try {
			// 特定于Xalan, 但这是最常见的XSLT引擎
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentAmount));
		}
		catch (IllegalArgumentException ignored) {
		}
	}

	/**
	 * 禁用所提供的{@link javax.xml.transform.Transformer}的缩进.
	 * 
	 * @param transformer 目标转换器
	 */
	public static void disableIndenting(Transformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		transformer.setOutputProperty(OutputKeys.INDENT, "no");
	}
}
