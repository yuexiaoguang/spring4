package org.springframework.mail.javamail;

import java.beans.PropertyEditorSupport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.springframework.util.StringUtils;

/**
 * {@code java.mail.internet.InternetAddress}的编辑器, 直接填充InternetAddress属性.
 *
 * <p>需要与带字符串参数的InternetAddress的构造函数具有相同的语法. 将空字符串转换为null值.
 */
public class InternetAddressEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(new InternetAddress(text));
			}
			catch (AddressException ex) {
				throw new IllegalArgumentException("Could not parse mail address: " + ex.getMessage());
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		InternetAddress value = (InternetAddress) getValue();
		return (value != null ? value.toUnicodeString() : "");
	}

}
