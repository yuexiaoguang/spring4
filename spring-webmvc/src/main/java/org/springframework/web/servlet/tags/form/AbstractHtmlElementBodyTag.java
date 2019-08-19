package org.springframework.web.servlet.tags.form;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;

import org.springframework.util.StringUtils;

/**
 * 方便的超类, 用于许多html标签, 使用{@link AbstractHtmlElementTag AbstractHtmlElementTag}的数据绑定功能呈现内容.
 * 子标签唯一需要做的是覆盖{@link #renderDefaultContent(TagWriter)}.
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementBodyTag extends AbstractHtmlElementTag implements BodyTag {

	private BodyContent bodyContent;

	private TagWriter tagWriter;


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		onWriteTagContent();
		this.tagWriter = tagWriter;
		if (shouldRender()) {
			exposeAttributes();
			return EVAL_BODY_BUFFERED;
		}
		else {
			return SKIP_BODY;
		}
	}

	/**
	 * 如果{@link #shouldRender 渲染}, 刷新所有缓冲的{@link BodyContent},
	 * 如果没有提供{@link BodyContent}, 则{@link #renderDefaultContent 呈现默认内容}.
	 * 
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_PAGE}结果
	 */
	@Override
	public int doEndTag() throws JspException {
		if (shouldRender()) {
			if (this.bodyContent != null && StringUtils.hasText(this.bodyContent.getString())) {
				renderFromBodyContent(this.bodyContent, this.tagWriter);
			}
			else {
				renderDefaultContent(this.tagWriter);
			}
		}
		return EVAL_PAGE;
	}

	/**
	 * 根据提供的{@link BodyContent}渲染标记内容.
	 * <p>默认实现只是直接{@link #flushBufferedBodyContent 刷新} {@link BodyContent}到输出.
	 * 子类可以选择覆盖它以向输出添加其他内容.
	 */
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		flushBufferedBodyContent(bodyContent);
	}

	/**
	 * 清理任何属性和存储的资源.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		removeAttributes();
		this.tagWriter = null;
		this.bodyContent = null;
	}


	//---------------------------------------------------------------------
	// Template methods
	//---------------------------------------------------------------------

	/**
	 * 在{@link #writeTagContent}的开头调用, 允许子类执行任何必要的前置条件检查或设置任务.
	 */
	protected void onWriteTagContent() {
	}

	/**
	 * 是否应该继续呈现此标记.
	 * 默认返回'{@code true}', 始终渲染, 如果子类提供条件渲染, 则可以覆盖它.
	 */
	protected boolean shouldRender() throws JspException {
		return true;
	}

	/**
	 * 在{@link #writeTagContent}期间调用, 允许子类根据需要向{@link javax.servlet.jsp.PageContext}添加任何属性.
	 */
	protected void exposeAttributes() throws JspException {
	}

	/**
	 * 由{@link #doFinally}调用, 允许子类根据需要从{@link javax.servlet.jsp.PageContext}中删除任何属性.
	 */
	protected void removeAttributes() {
	}

	/**
	 * 用户自定义错误消息的输出 - 将缓冲的内容刷新到主{@link javax.servlet.jsp.JspWriter}.
	 */
	protected void flushBufferedBodyContent(BodyContent bodyContent) throws JspException {
		try {
			bodyContent.writeOut(bodyContent.getEnclosingWriter());
		}
		catch (IOException ex) {
			throw new JspException("Unable to write buffered body content.", ex);
		}
	}

	protected abstract void renderDefaultContent(TagWriter tagWriter) throws JspException;


	//---------------------------------------------------------------------
	// BodyTag implementation
	//---------------------------------------------------------------------

	@Override
	public void doInitBody() throws JspException {
		// no op
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

}
