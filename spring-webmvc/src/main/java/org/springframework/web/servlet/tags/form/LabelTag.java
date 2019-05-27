package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code label}' element
 * that defines text that is associated with a single form element.
 *
 * <p>See the "formTags" showcase application that ships with the
 * full Spring distribution for an example of this class in action.
 */
@SuppressWarnings("serial")
public class LabelTag extends AbstractHtmlElementTag {

	/**
	 * The HTML '{@code label}' tag.
	 */
	private static final String LABEL_TAG = "label";

	/**
	 * The name of the '{@code for}' attribute.
	 */
	private static final String FOR_ATTRIBUTE = "for";


	/**
	 * The {@link TagWriter} instance being used.
	 * <p>Stored so we can close the tag on {@link #doEndTag()}.
	 */
	private TagWriter tagWriter;

	/**
	 * The value of the '{@code for}' attribute.
	 */
	private String forId;


	/**
	 * Set the value of the '{@code for}' attribute.
	 * <p>Defaults to the value of {@link #getPath}; may be a runtime expression.
	 * @throws IllegalArgumentException if the supplied value is {@code null}
	 */
	public void setFor(String forId) {
		Assert.notNull(forId, "'forId' must not be null");
		this.forId = forId;
	}

	/**
	 * Get the value of the '{@code id}' attribute.
	 * <p>May be a runtime expression.
	 */
	public String getFor() {
		return this.forId;
	}


	/**
	 * Writes the opening '{@code label}' tag and forces a block tag so
	 * that body content is written correctly.
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
	 * Overrides {@code #getName()} to always return {@code null},
	 * because the '{@code name}' attribute is not supported by the
	 * '{@code label}' tag.
	 * @return the value for the HTML '{@code name}' attribute
	 */
	@Override
	protected String getName() throws JspException {
		// This also suppresses the 'id' attribute (which is okay for a <label/>)
		return null;
	}

	/**
	 * Determine the '{@code for}' attribute value for this tag,
	 * autogenerating one if none specified.
	 * @see #getFor()
	 * @see #autogenerateFor()
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
	 * Autogenerate the '{@code for}' attribute value for this tag.
	 * <p>The default implementation delegates to {@link #getPropertyPath()},
	 * deleting invalid characters (such as "[" or "]").
	 */
	protected String autogenerateFor() throws JspException {
		return StringUtils.deleteAny(getPropertyPath(), "[]");
	}

	/**
	 * Close the '{@code label}' tag.
	 */
	@Override
	public int doEndTag() throws JspException {
		this.tagWriter.endTag();
		return EVAL_PAGE;
	}

	/**
	 * Disposes of the {@link TagWriter} instance.
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		this.tagWriter = null;
	}

}
