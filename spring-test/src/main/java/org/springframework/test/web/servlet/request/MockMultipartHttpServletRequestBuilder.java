package org.springframework.test.web.servlet.request;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletContext;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

/**
 * {@link MockMultipartHttpServletRequest}的默认构建器.
 */
public class MockMultipartHttpServletRequestBuilder extends MockHttpServletRequestBuilder {

	private final List<MockMultipartFile> files = new ArrayList<MockMultipartFile>();


	/**
	 * 在{@link MockMvcRequestBuilders}中使用静态工厂方法.
	 * <p>有关初始化{@code MockMultipartHttpServletRequest}的其他方法,
	 * 请参阅{@link #with(RequestPostProcessor)}和{@link RequestPostProcessor}扩展点.
	 * 
	 * @param urlTemplate URL模板; 生成的URL将被编码
	 * @param uriVariables 零个或多个URI变量
	 */
	MockMultipartHttpServletRequestBuilder(String urlTemplate, Object... uriVariables) {
		super(HttpMethod.POST, urlTemplate, uriVariables);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}

	/**
	 * 在{@link MockMvcRequestBuilders}中使用静态工厂方法.
	 * <p>有关初始化{@code MockMultipartHttpServletRequest}的其他方法,
	 * 请参阅{@link #with(RequestPostProcessor)}和{@link RequestPostProcessor}扩展点.
	 * 
	 * @param uri the URL
	 */
	MockMultipartHttpServletRequestBuilder(URI uri) {
		super(HttpMethod.POST, uri);
		super.contentType(MediaType.MULTIPART_FORM_DATA);
	}


	/**
	 * 使用给定内容创建一个新的MockMultipartFile.
	 * 
	 * @param name 文件名
	 * @param content 文件的内容
	 */
	public MockMultipartHttpServletRequestBuilder file(String name, byte[] content) {
		this.files.add(new MockMultipartFile(name, content));
		return this;
	}

	/**
	 * 添加给定的MockMultipartFile.
	 * 
	 * @param file 文件
	 */
	public MockMultipartHttpServletRequestBuilder file(MockMultipartFile file) {
		this.files.add(file);
		return this;
	}

	@Override
	public Object merge(Object parent) {
		if (parent == null) {
			return this;
		}
		if (parent instanceof MockHttpServletRequestBuilder) {
			super.merge(parent);
			if (parent instanceof MockMultipartHttpServletRequestBuilder) {
				MockMultipartHttpServletRequestBuilder parentBuilder = (MockMultipartHttpServletRequestBuilder) parent;
				this.files.addAll(parentBuilder.files);
			}
		}
		else {
			throw new IllegalArgumentException("Cannot merge with [" + parent.getClass().getName() + "]");
		}
		return this;
	}

	/**
	 * 根据提供的{@code ServletContext}和添加到此构建器的{@code MockMultipartFiles}创建一个新的{@link MockMultipartHttpServletRequest}.
	 */
	@Override
	protected final MockHttpServletRequest createServletRequest(ServletContext servletContext) {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest(servletContext);
		for (MockMultipartFile file : this.files) {
			request.addFile(file);
		}
		return request;
	}

}
