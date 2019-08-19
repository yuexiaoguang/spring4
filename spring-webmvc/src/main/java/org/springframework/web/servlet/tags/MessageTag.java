package org.springframework.web.servlet.tags;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

/**
 * 自定义JSP标记, 用于在此页面的范围内查找消息.
 * 使用ApplicationContext解析消息, 从而支持国际化.
 *
 * <p>检测此标记实例, 页面级别或{@code web.xml}级别的HTML转义设置.
 * 也可以应用JavaScript转义.
 *
 * <p>如果未设置或无法解析"code", 则"text"将用作默认消息.
 * 因此, 此标记还可用于任何文本的HTML转义.
 *
 * <p>可以通过{@link #setArguments(Object) arguments}属性或使用嵌套的{@code <spring:argument>}标记指定消息参数.
 */
@SuppressWarnings("serial")
public class MessageTag extends HtmlEscapingAwareTag implements ArgumentAware {

	/**
	 * 用于拆分参数String的默认分隔符: 逗号 (",")
	 */
	public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";


	private MessageSourceResolvable message;

	private String code;

	private Object arguments;

	private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

	private List<Object> nestedArguments;

	private String text;

	private String var;

	private String scope = TagUtils.SCOPE_PAGE;

	private boolean javaScriptEscape = false;


	/**
	 * 设置此标记的MessageSourceResolvable.
	 * <p>如果指定了MessageSourceResolvable, 它将有效地覆盖此标记上指定的任何代码, 参数或文本.
	 */
	public void setMessage(MessageSourceResolvable message) {
		this.message = message;
	}

	/**
	 * 设置此标记的消息代码.
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * 设置此标记可选的消息参数, 以逗号分隔的String (每个String参数可以包含JSP EL),
	 * Object数组 (用作参数数组), 或单个Object (用作单个参数).
	 */
	public void setArguments(Object arguments) {
		this.arguments = arguments;
	}

	/**
	 * 设置用于拆分参数String的分隔符.
	 * 默认为逗号 (",").
	 */
	public void setArgumentSeparator(String argumentSeparator) {
		this.argumentSeparator = argumentSeparator;
	}

	@Override
	public void addArgument(Object argument) throws JspTagException {
		this.nestedArguments.add(argument);
	}

	/**
	 * 设置此标记的消息文本.
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * 设置PageContext属性名称, 在该名称下公开包含已解析消息的变量.
	 */
	public void setVar(String var) {
		this.var = var;
	}

	/**
	 * 设置将变量导出到的范围.
	 * 默认为 SCOPE_PAGE ("page").
	 */
	public void setScope(String scope) {
		this.scope = scope;
	}

	/**
	 * 设置此标记的JavaScript转义.
	 * 默认为 "false".
	 */
	public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
		this.javaScriptEscape = javaScriptEscape;
	}


	@Override
	protected final int doStartTagInternal() throws JspException, IOException {
		this.nestedArguments = new LinkedList<Object>();
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 解析消息, 在需要时将其转义, 并将其写入页面 (或将其作为变量公开).
	 */
	@Override
	public int doEndTag() throws JspException {
		try {
			// Resolve the unescaped message.
			String msg = resolveMessage();

			// HTML and/or JavaScript escape, if demanded.
			msg = htmlEscape(msg);
			msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;

			// Expose as variable, if demanded, else write to the page.
			if (this.var != null) {
				pageContext.setAttribute(this.var, msg, TagUtils.getScope(this.scope));
			}
			else {
				writeMessage(msg);
			}

			return EVAL_PAGE;
		}
		catch (IOException ex) {
			throw new JspTagException(ex.getMessage(), ex);
		}
		catch (NoSuchMessageException ex) {
			throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
		}
	}

	@Override
	public void release() {
		super.release();
		this.arguments = null;
	}

	/**
	 * 将指定的消息解析为具体消息String.
	 * 返回的消息String应该是未转义的.
	 */
	protected String resolveMessage() throws JspException, NoSuchMessageException {
		MessageSource messageSource = getMessageSource();
		if (messageSource == null) {
			throw new JspTagException("No corresponding MessageSource found");
		}

		// 评估指定的 MessageSourceResolvable.
		if (this.message != null) {
			// We have a given MessageSourceResolvable.
			return messageSource.getMessage(this.message, getRequestContext().getLocale());
		}

		if (this.code != null || this.text != null) {
			// 需要解析代码或默认文本.
			Object[] argumentsArray = resolveArguments(this.arguments);
			if (!this.nestedArguments.isEmpty()) {
				argumentsArray = appendArguments(argumentsArray, this.nestedArguments.toArray());
			}

			if (this.text != null) {
				// We have a fallback text to consider.
				return messageSource.getMessage(
						this.code, argumentsArray, this.text, getRequestContext().getLocale());
			}
			else {
				// We have no fallback text to consider.
				return messageSource.getMessage(
						this.code, argumentsArray, getRequestContext().getLocale());
			}
		}

		// All we have is a specified literal text.
		return this.text;
	}

	private Object[] appendArguments(Object[] sourceArguments, Object[] additionalArguments) {
		if (ObjectUtils.isEmpty(sourceArguments)) {
			return additionalArguments;
		}
		Object[] arguments = new Object[sourceArguments.length + additionalArguments.length];
		System.arraycopy(sourceArguments, 0, arguments, 0, sourceArguments.length);
		System.arraycopy(additionalArguments, 0, arguments, sourceArguments.length, additionalArguments.length);
		return arguments;
	}

	/**
	 * 将给定的参数Object解析为参数数组.
	 * 
	 * @param arguments 指定的参数Object
	 * 
	 * @return 解析后的参数
	 * @throws JspException 如果参数转换失败
	 */
	protected Object[] resolveArguments(Object arguments) throws JspException {
		if (arguments instanceof String) {
			String[] stringArray =
					StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
			if (stringArray.length == 1) {
				Object argument = stringArray[0];
				if (argument != null && argument.getClass().isArray()) {
					return ObjectUtils.toObjectArray(argument);
				}
				else {
					return new Object[] {argument};
				}
			}
			else {
				return stringArray;
			}
		}
		else if (arguments instanceof Object[]) {
			return (Object[]) arguments;
		}
		else if (arguments instanceof Collection) {
			return ((Collection<?>) arguments).toArray();
		}
		else if (arguments != null) {
			// Assume a single argument object.
			return new Object[] {arguments};
		}
		else {
			return null;
		}
	}

	/**
	 * 将消息写入页面.
	 * <p>可以在子类中重写, e.g. 用于测试.
	 * 
	 * @param msg 要写入的消息
	 * 
	 * @throws IOException 如果写入失败
	 */
	protected void writeMessage(String msg) throws IOException {
		pageContext.getOut().write(String.valueOf(msg));
	}

	/**
	 * 使用当前的RequestContext的应用程序上下文.
	 */
	protected MessageSource getMessageSource() {
		return getRequestContext().getMessageSource();
	}

	/**
	 * 返回默认异常消息.
	 */
	protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
		return ex.getMessage();
	}

}
