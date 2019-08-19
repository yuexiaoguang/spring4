package org.springframework.web.servlet.tags.form;

import java.io.IOException;
import java.io.Writer;
import java.util.Stack;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于将HTML内容写入{@link Writer}实例的工具类.
 *
 * <p>旨在支持JSP标记库的输出.
 */
public class TagWriter {

	/**
	 * 要写入的{@link SafeWriter}.
	 */
	private final SafeWriter writer;

	/**
	 * 存储{@link TagStateEntry 标记状态}. 堆栈模型自然支持标记嵌套.
	 */
	private final Stack<TagStateEntry> tagState = new Stack<TagStateEntry>();


	/**
	 * 创建一个{@link TagWriter}类的新实例, 该类写入提供的{@link PageContext}.
	 * 
	 * @param pageContext 从中获取{@link Writer}的JSP PageContext
	 */
	public TagWriter(PageContext pageContext) {
		Assert.notNull(pageContext, "PageContext must not be null");
		this.writer = new SafeWriter(pageContext);
	}

	/**
	 * 创建一个{@link TagWriter}类的新实例, 该类写入提供的{@link Writer}.
	 * 
	 * @param writer 要写入标记内容的{@link Writer}
	 */
	public TagWriter(Writer writer) {
		Assert.notNull(writer, "Writer must not be null");
		this.writer = new SafeWriter(writer);
	}


	/**
	 * 使用提供的名称启动新标记.
	 * 使标记保持打开状态, 以便可以将属性, 内部文本或嵌套标记写入其中.
	 */
	public void startTag(String tagName) throws JspException {
		if (inTag()) {
			closeTagAndMarkAsBlock();
		}
		push(tagName);
		this.writer.append("<").append(tagName);
	}

	/**
	 * 写入具有指定名称和值的HTML属性.
	 * <p>确保在写入任何内部文本或嵌套标记<strong>之前</strong>, 写入所有属性.
	 * 
	 * @throws IllegalStateException 如果开始标记已关闭
	 */
	public void writeAttribute(String attributeName, String attributeValue) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName).append("=\"")
				.append(attributeValue).append("\"");
	}

	/**
	 * 如果提供的值不是{@code null}或零长度, 则写入HTML属性.
	 */
	public void writeOptionalAttributeValue(String attributeName, String attributeValue) throws JspException {
		if (StringUtils.hasText(attributeValue)) {
			writeAttribute(attributeName, attributeValue);
		}
	}

	/**
	 * 关闭当前的开始标记, 并将提供的值附加为内部文本.
	 * 
	 * @throws IllegalStateException 如果没有标记打开
	 */
	public void appendValue(String value) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write tag value. No open tag available.");
		}
		closeTagAndMarkAsBlock();
		this.writer.append(value);
	}


	/**
	 * 指示应关闭当前打开的标记, 并将其标记为块级元素.
	 * <p>当计划在当前{@link TagWriter}的上下文之外的正文中, 写入其他内容时很有用.
	 */
	public void forceBlock() throws JspException {
		if (currentState().isBlockTag()) {
			return; // 只是忽略, 因为已经是块级
		}
		closeTagAndMarkAsBlock();
	}

	/**
	 * 关闭当前的标记.
	 * <p>如果没有写入内部文本或嵌套标记, 则正确写入空标记.
	 */
	public void endTag() throws JspException {
		endTag(false);
	}

	/**
	 * 关闭当前标记, 允许强制执行完整的结束标记.
	 * <p>如果没有写入内部文本或嵌套标记, 则正确写入空标记.
	 * 
	 * @param enforceClosingTag 是否应该在任何情况下呈现完整的结束标记, 即使在非块标记的情况下也是如此
	 */
	public void endTag(boolean enforceClosingTag) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write end of tag. No open tag available.");
		}
		boolean renderClosingTag = true;
		if (!currentState().isBlockTag()) {
			// 打开的标记仍需要关闭...
			if (enforceClosingTag) {
				this.writer.append(">");
			}
			else {
				this.writer.append("/>");
				renderClosingTag = false;
			}
		}
		if (renderClosingTag) {
			this.writer.append("</").append(currentState().getTagName()).append(">");
		}
		this.tagState.pop();
	}


	/**
	 * 将提供的标记名称添加到{@link #tagState 标记状态}.
	 */
	private void push(String tagName) {
		this.tagState.push(new TagStateEntry(tagName));
	}

	/**
	 * 关闭当前的开始标记, 并将其标记为块标记.
	 */
	private void closeTagAndMarkAsBlock() throws JspException {
		if (!currentState().isBlockTag()) {
			currentState().markAsBlockTag();
			this.writer.append(">");
		}
	}

	private boolean inTag() {
		return !this.tagState.isEmpty();
	}

	private TagStateEntry currentState() {
		return this.tagState.peek();
	}


	/**
	 * 保存有关标记及其呈现行为的状态.
	 */
	private static class TagStateEntry {

		private final String tagName;

		private boolean blockTag;

		public TagStateEntry(String tagName) {
			this.tagName = tagName;
		}

		public String getTagName() {
			return this.tagName;
		}

		public void markAsBlockTag() {
			this.blockTag = true;
		}

		public boolean isBlockTag() {
			return this.blockTag;
		}
	}


	/**
	 * 简单的{@link Writer}包装器, 包装{@link JspException JspExceptions}中的所有{@link IOException IOExceptions}.
	 */
	private static final class SafeWriter {

		private PageContext pageContext;

		private Writer writer;

		public SafeWriter(PageContext pageContext) {
			this.pageContext = pageContext;
		}

		public SafeWriter(Writer writer) {
			this.writer = writer;
		}

		public SafeWriter append(String value) throws JspException {
			try {
				getWriterToUse().write(String.valueOf(value));
				return this;
			}
			catch (IOException ex) {
				throw new JspException("Unable to write to JspWriter", ex);
			}
		}

		private Writer getWriterToUse() {
			return (this.pageContext != null ? this.pageContext.getOut() : this.writer);
		}
	}

}
