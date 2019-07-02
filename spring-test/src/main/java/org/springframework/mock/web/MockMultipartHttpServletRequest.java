package org.springframework.mock.web;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

/**
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest}接口的模拟实现.
 *
 * <p>从Spring 4.0开始, 这组模拟是在Servlet 3.0基线上设计的.
 *
 * <p>用于测试访问multipart上传的应用程序控制器.
 * {@link MockMultipartFile}可用于使用文件填充这些模拟请求.
 */
public class MockMultipartHttpServletRequest extends MockHttpServletRequest implements MultipartHttpServletRequest {

	private final MultiValueMap<String, MultipartFile> multipartFiles =
			new LinkedMultiValueMap<String, MultipartFile>();


	/**
	 * 使用默认的{@link MockServletContext}.
	 */
	public MockMultipartHttpServletRequest() {
		this(null);
	}

	/**
	 * @param servletContext 运行请求的ServletContext (可能是{@code null}以使用默认的{@link MockServletContext})
	 */
	public MockMultipartHttpServletRequest(ServletContext servletContext) {
		super(servletContext);
		setMethod("POST");
		setContentType("multipart/form-data");
	}


	/**
	 * 将文件添加到此请求.
	 * multipart表单中的参数名称取自{@link MultipartFile#getName()}.
	 * 
	 * @param file 要添加的multipart文件
	 */
	public void addFile(MultipartFile file) {
		Assert.notNull(file, "MultipartFile must not be null");
		this.multipartFiles.add(file.getName(), file);
	}

	@Override
	public Iterator<String> getFileNames() {
		return this.multipartFiles.keySet().iterator();
	}

	@Override
	public MultipartFile getFile(String name) {
		return this.multipartFiles.getFirst(name);
	}

	@Override
	public List<MultipartFile> getFiles(String name) {
		List<MultipartFile> multipartFiles = this.multipartFiles.get(name);
		if (multipartFiles != null) {
			return multipartFiles;
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, MultipartFile> getFileMap() {
		return this.multipartFiles.toSingleValueMap();
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return new LinkedMultiValueMap<String, MultipartFile>(this.multipartFiles);
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			return file.getContentType();
		}
		else {
			return null;
		}
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.resolve(getMethod());
	}

	@Override
	public HttpHeaders getRequestHeaders() {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.put(headerName, Collections.list(getHeaders(headerName)));
		}
		return headers;
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		String contentType = getMultipartContentType(paramOrFileName);
		if (contentType != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Content-Type", contentType);
			return headers;
		}
		else {
			return null;
		}
	}

}
