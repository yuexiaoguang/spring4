package org.springframework.web.servlet.tags.form;

import javax.servlet.jsp.JspException;

/**
 * 用于呈现'{@code type}'为'{@code password}'的 HTML '{@code input}'元素的数据绑定感知JSP标记.
 */
@SuppressWarnings("serial")
public class PasswordInputTag extends InputTag {

	private boolean showPassword = false;


	/**
	 * 是否呈现的密码值?
	 * 
	 * @param showPassword {@code true} 如果要呈现密码值
	 */
	public void setShowPassword(boolean showPassword) {
		this.showPassword = showPassword;
	}

	/**
	 * 是否呈现的密码值?
	 * 
	 * @return {@code true} 如果要呈现密码值
	 */
	public boolean isShowPassword() {
		return this.showPassword;
	}


	/**
	 * 标记"type"为非法动态属性.
	 */
	@Override
	protected boolean isValidDynamicAttribute(String localName, Object value) {
		return !"type".equals(localName);
	}

	/**
	 * 返回'{@code password}', 使呈现的HTML '{@code input}'元素具有'{@code password}'的'{@code type}'.
	 */
	@Override
	protected String getType() {
		return "password";
	}

	/**
	 * 如果{@link #setShowPassword(boolean) 'showPassword'}属性值为{@link Boolean#TRUE true},
	 * {@link PasswordInputTag}只会写入它的值.
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
