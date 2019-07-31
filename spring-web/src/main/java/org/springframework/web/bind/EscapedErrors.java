package org.springframework.web.bind;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

/**
 * Error包装器, 它将自动HTML转义添加到包装的实例, 以便在HTML视图中使用.
 * 可以通过RequestContext的{@code getErrors}方法轻松检索.
 *
 * <p>请注意, BindTag <i>不</i>使用此类以避免不必要地创建ObjectError实例.
 * 它只是转义了复制到相应BindStatus实例中的消息和值.
 */
public class EscapedErrors implements Errors {

	private final Errors source;


	/**
	 * 为给定的源实例创建新的EscapedErrors实例.
	 */
	public EscapedErrors(Errors source) {
		Assert.notNull(source, "Errors source must not be null");
		this.source = source;
	}

	public Errors getSource() {
		return this.source;
	}


	@Override
	public String getObjectName() {
		return this.source.getObjectName();
	}

	@Override
	public void setNestedPath(String nestedPath) {
		this.source.setNestedPath(nestedPath);
	}

	@Override
	public String getNestedPath() {
		return this.source.getNestedPath();
	}

	@Override
	public void pushNestedPath(String subPath) {
		this.source.pushNestedPath(subPath);
	}

	@Override
	public void popNestedPath() throws IllegalStateException {
		this.source.popNestedPath();
	}


	@Override
	public void reject(String errorCode) {
		this.source.reject(errorCode);
	}

	@Override
	public void reject(String errorCode, String defaultMessage) {
		this.source.reject(errorCode, defaultMessage);
	}

	@Override
	public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
		this.source.reject(errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void rejectValue(String field, String errorCode) {
		this.source.rejectValue(field, errorCode);
	}

	@Override
	public void rejectValue(String field, String errorCode, String defaultMessage) {
		this.source.rejectValue(field, errorCode, defaultMessage);
	}

	@Override
	public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
		this.source.rejectValue(field, errorCode, errorArgs, defaultMessage);
	}

	@Override
	public void addAllErrors(Errors errors) {
		this.source.addAllErrors(errors);
	}


	@Override
	public boolean hasErrors() {
		return this.source.hasErrors();
	}

	@Override
	public int getErrorCount() {
		return this.source.getErrorCount();
	}

	@Override
	public List<ObjectError> getAllErrors() {
		return escapeObjectErrors(this.source.getAllErrors());
	}

	@Override
	public boolean hasGlobalErrors() {
		return this.source.hasGlobalErrors();
	}

	@Override
	public int getGlobalErrorCount() {
		return this.source.getGlobalErrorCount();
	}

	@Override
	public List<ObjectError> getGlobalErrors() {
		return escapeObjectErrors(this.source.getGlobalErrors());
	}

	@Override
	public ObjectError getGlobalError() {
		return escapeObjectError(this.source.getGlobalError());
	}

	@Override
	public boolean hasFieldErrors() {
		return this.source.hasFieldErrors();
	}

	@Override
	public int getFieldErrorCount() {
		return this.source.getFieldErrorCount();
	}

	@Override
	public List<FieldError> getFieldErrors() {
		return this.source.getFieldErrors();
	}

	@Override
	public FieldError getFieldError() {
		return this.source.getFieldError();
	}

	@Override
	public boolean hasFieldErrors(String field) {
		return this.source.hasFieldErrors(field);
	}

	@Override
	public int getFieldErrorCount(String field) {
		return this.source.getFieldErrorCount(field);
	}

	@Override
	public List<FieldError> getFieldErrors(String field) {
		return escapeObjectErrors(this.source.getFieldErrors(field));
	}

	@Override
	public FieldError getFieldError(String field) {
		return escapeObjectError(this.source.getFieldError(field));
	}

	@Override
	public Object getFieldValue(String field) {
		Object value = this.source.getFieldValue(field);
		return (value instanceof String ? HtmlUtils.htmlEscape((String) value) : value);
	}

	@Override
	public Class<?> getFieldType(String field) {
		return this.source.getFieldType(field);
	}

	@SuppressWarnings("unchecked")
	private <T extends ObjectError> T escapeObjectError(T source) {
		if (source == null) {
			return null;
		}
		if (source instanceof FieldError) {
			FieldError fieldError = (FieldError) source;
			Object value = fieldError.getRejectedValue();
			if (value instanceof String) {
				value = HtmlUtils.htmlEscape((String) value);
			}
			return (T) new FieldError(
					fieldError.getObjectName(), fieldError.getField(), value,
					fieldError.isBindingFailure(), fieldError.getCodes(),
					fieldError.getArguments(), HtmlUtils.htmlEscape(fieldError.getDefaultMessage()));
		}
		else {
			return (T) new ObjectError(
					source.getObjectName(), source.getCodes(), source.getArguments(),
					HtmlUtils.htmlEscape(source.getDefaultMessage()));
		}
	}

	private <T extends ObjectError> List<T> escapeObjectErrors(List<T> source) {
		List<T> escaped = new ArrayList<T>(source.size());
		for (T objectError : source) {
			escaped.add(escapeObjectError(objectError));
		}
		return escaped;
	}

}
