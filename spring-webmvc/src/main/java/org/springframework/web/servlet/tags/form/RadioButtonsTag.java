package org.springframework.web.servlet.tags.form;

/**
 * 用于呈现多个'{@code type}'为'{@code radio}'的 HTML '{@code input}'元素的数据绑定感知JSP标记.
 *
 * <p>如果配置的{@link #setItems(Object) 值}与绑定值匹配, 则渲染的元素将标记为'checked'.
 */
@SuppressWarnings("serial")
public class RadioButtonsTag extends AbstractMultiCheckedElementTag {

	@Override
	protected String getInputType() {
		return "radio";
	}

}
