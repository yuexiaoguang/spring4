package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 用于呈现 HTML '{@code label}'元素的数据绑定感知JSP标记, 该元素定义与单个表单元素关联的文本.
 *
 * <p>请参阅"formTags"展示应用程序, 该应用程序附带完整的Spring发行版, 以获取此类的实例.
 */
@SuppressWarnings("serial")
public class LabelTag extends AbstractHtmlElementTag {

	/**
	 * HTML '{@code label}'标记.
	 */
	private static final String LABEL_TAG = "label";

	/**
	 * '{@code for}'属性的名称.
	 */
	private static final String FOR_ATTRIBUTE = "for";


	/**
	 * 正在使用的{@link TagWriter}实例.
	 * <p>存储, 以便可以关闭{@link #doEndTag()}上的标记.
	 */
	private TagWriter tagWriter;

	/**
	 * '{@code for}'属性的值.
	 */
	private String forId;


	/**
	 * 设置'{@code for}'属性的值.
	 * <p>默认为{@link #getPath}的值; 可以是运行时表达式.
	 * 
	 * @throws IllegalArgumentException 如果提供的值是{@code null}
	 */
	public void setFor(String forId) {
		Assert.notNull(forId, "'forId' must not be null");
		this.forId = forId;
	}

	/**
	 * 获取'{@code id}'属性的值.
	 * <p>可能是运行时表达式.
	 */
	public String getFor() {
		return this.forId;
	}


	/**
	 * 写入开头的'{@code label}'标记并强制块标记, 以便正确写入正文内容.
	 * 
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_BODY_INCLUDE}
	 */
	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		tagWriter.startTag(LABEL_TAG);
		tagWriter.writeAttribute(FOR_ATTRIBUTE, resolveFor());
		writeDefaultAttributes(tagWriter);
		tagWriter.forceBlock();
		this.tagWriter = tagWriter;
		return EVAL_BODY_INCLUDE;
	}

	/**
	 * 覆盖{@code #getName()}以始终返回{@code null}, 因为'{@code label}'标签不支持'{@code name}'属性.
	 * 
	 * @return HTML '{@code name}'属性的值
	 */
	@Override
	protected String getName() throws JspException {
		// 也抑制了'id'属性 (可以用于<label/>)
		return null;
	}

	/**
	 * 确定此标记的'{@code for}'属性值, 如果未指定, 则自动生成一个.
	 */
	protected String resolveFor() throws JspException {
		if (StringUtils.hasText(this.forId)) {
			return getDisplayString(evaluate(FOR_ATTRIBUTE, this.forId));
		}
		else {
			return autogenerateFor();
		}
	}

	/**
	 * 自动生成此标记的'{@code for}'属性值.
	 * <p>默认实现委托给{@link #getPropertyPath()}, 删除无效的字符 (例如 "[" 或 "]").
	 */
	protected String autogenerateFor() throws JspException {
		return StringUtils.deleteAny(getPropertyPath(), "[]");
	}

	/**
	 * 关闭'{@code label}'标记.
	 */
	@Override
	public int doEndTag() throws JspException {
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * 处置{@link TagWriter}实例.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
	}

}
