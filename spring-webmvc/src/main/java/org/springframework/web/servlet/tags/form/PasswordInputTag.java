package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * Databinding-aware JSP tag for rendering an HTML '{@code input}'
 * element with a '{@code type}' of '{@code password}'.
 */
@SuppressWarnings("serial")
public class PasswordInputTag extends InputTag {

	private boolean showPassword = false;


	/**
	 * Is the password value to be rendered?
	 * @param showPassword {@code true} if the password value is to be rendered
	 */
	public void setShowPassword(boolean showPassword) {
		this.showPassword = showPassword;
	}

	/**
	 * Is the password value to be rendered?
	 * @return {@code true} if the password value to be rendered
	 */
	public boolean isShowPassword() {
		return this.showPassword;
	}


	/**
	 * Flags "type" as an illegal dynamic attribute.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * Return '{@code password}' causing the rendered HTML '{@code input}'
	 * element to have a '{@code type}' of '{@code password}'.
	 */
	@Override
	protected String getType() {
		return "password";
	}

	/**
	 * The {@link PasswordInputTag} only writes it's value if the
	 * {@link #setShowPassword(boolean) 'showPassword'} property value is
	 * {@link Boolean#TRUE true}.
	 */
	@Override
	protected void writeValue(TagWriter tagWriter) throws JspException {
		if (this.showPassword) {
			super.writeValue(tagWriter);
		}
		else {
			tagWriter.writeAttribute("value", processFieldValue(getName(), "", getType()));
		}
	}

}
