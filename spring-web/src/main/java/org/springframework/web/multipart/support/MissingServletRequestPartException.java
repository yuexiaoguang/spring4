package org.springframework.web.multipart.support;

import javax.servlet.ServletException;

import org.springframework.web.multipart.MultipartResolver;

/**
 * 当无法找到由其名称标识的"multipart/form-data"请求的part时引发.
 *
 * <p>这可能是因为请求不是 multipart/form-data 请求, 因为该part不存在于请求中,
 * 或者因为Web应用程序未正确配置以处理multipart请求, e.g. 没有{@link MultipartResolver}.
 */
@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException {

	private final String partName;


	public MissingServletRequestPartException(String partName) {
		super("Required request part '" + partName + "' is not present");
		this.partName = partName;
	}


	public String getRequestPartName() {
		return this.partName;
	}

}
