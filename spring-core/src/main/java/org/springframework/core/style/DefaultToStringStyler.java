package org.springframework.core.style;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * Spring的默认{@code toString()}样式.
 *
 * <p>根据Spring约定, {@link ToStringCreator}使用此类以一致的方式设置{@code toString()}输出的样式.
 */
public class DefaultToStringStyler implements ToStringStyler {

	private final ValueStyler valueStyler;


	/**
	 * @param valueStyler 要使用的ValueStyler
	 */
	public DefaultToStringStyler(ValueStyler valueStyler) {
		Assert.notNull(valueStyler, "ValueStyler must not be null");
		this.valueStyler = valueStyler;
	}

	/**
	 * 返回此ToStringStyler使用的ValueStyler.
	 */
	protected final ValueStyler getValueStyler() {
		return this.valueStyler;
	}


	@Override
	public void styleStart(StringBuilder buffer, Object obj) {
		if (!obj.getClass().isArray()) {
			buffer.append('[').append(ClassUtils.getShortName(obj.getClass()));
			styleIdentityHashCode(buffer, obj);
		}
		else {
			buffer.append('[');
			styleIdentityHashCode(buffer, obj);
			buffer.append(' ');
			styleValue(buffer, obj);
		}
	}

	private void styleIdentityHashCode(StringBuilder buffer, Object obj) {
		buffer.append('@');
		buffer.append(ObjectUtils.getIdentityHexString(obj));
	}

	@Override
	public void styleEnd(StringBuilder buffer, Object o) {
		buffer.append(']');
	}

	@Override
	public void styleField(StringBuilder buffer, String fieldName, Object value) {
		styleFieldStart(buffer, fieldName);
		styleValue(buffer, value);
		styleFieldEnd(buffer, fieldName);
	}

	protected void styleFieldStart(StringBuilder buffer, String fieldName) {
		buffer.append(' ').append(fieldName).append(" = ");
	}

	protected void styleFieldEnd(StringBuilder buffer, String fieldName) {
	}

	@Override
	public void styleValue(StringBuilder buffer, Object value) {
		buffer.append(this.valueStyler.style(value));
	}

	@Override
	public void styleFieldSeparator(StringBuilder buffer) {
		buffer.append(',');
	}

}
