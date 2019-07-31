package org.springframework.web.multipart.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.springframework.http.HttpHeaders;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Spring MultipartHttpServletRequest适配器, 包装Servlet 3.0 HttpServletRequest及其Part对象.
 * 参数通过本机请求的getParameter方法公开 - 没有任何自定义处理.
 */
public class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {

	private static final String CONTENT_DISPOSITION = "content-disposition";

	private static final String FILENAME_KEY = "filename=";

	private static final String FILENAME_WITH_CHARSET_KEY = "filename*=";

	private static final Charset US_ASCII = Charset.forName("us-ascii");


	private Set<String> multipartParameterNames;


	/**
	 * 立即解析multipart内容.
	 * 
	 * @param request 要包装的servlet请求
	 * 
	 * @throws MultipartException 如果解析失败
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
		this(request, false);
	}

	/**
	 * @param request 要包装的servlet请求
	 * @param lazyParsing 是否应该在首次访问multipart文件或参数时延迟触发multipart解析
	 * 
	 * @throws MultipartException 如果立即解析尝试失败
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request, boolean lazyParsing)
			throws MultipartException {

		super(request);
		if (!lazyParsing) {
			parseRequest(request);
		}
	}


	private void parseRequest(HttpServletRequest request) {
		try {
			Collection<Part> parts = request.getParts();
			this.multipartParameterNames = new LinkedHashSet<String>(parts.size());
			MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<String, MultipartFile>(parts.size());
			for (Part part : parts) {
				String disposition = part.getHeader(CONTENT_DISPOSITION);
				String filename = extractFilename(disposition);
				if (filename == null) {
					filename = extractFilenameWithCharset(disposition);
				}
				if (filename != null) {
					files.add(part.getName(), new StandardMultipartFile(part, filename));
				}
				else {
					this.multipartParameterNames.add(part.getName());
				}
			}
			setMultipartFiles(files);
		}
		catch (Throwable ex) {
			throw new MultipartException("Could not parse multipart servlet request", ex);
		}
	}

	private String extractFilename(String contentDisposition, String key) {
		if (contentDisposition == null) {
			return null;
		}
		int startIndex = contentDisposition.indexOf(key);
		if (startIndex == -1) {
			return null;
		}
		String filename = contentDisposition.substring(startIndex + key.length());
		if (filename.startsWith("\"")) {
			int endIndex = filename.indexOf("\"", 1);
			if (endIndex != -1) {
				return filename.substring(1, endIndex);
			}
		}
		else {
			int endIndex = filename.indexOf(";");
			if (endIndex != -1) {
				return filename.substring(0, endIndex);
			}
		}
		return filename;
	}

	private String extractFilename(String contentDisposition) {
		return extractFilename(contentDisposition, FILENAME_KEY);
	}

	private String extractFilenameWithCharset(String contentDisposition) {
		String filename = extractFilename(contentDisposition, FILENAME_WITH_CHARSET_KEY);
		if (filename == null) {
			return null;
		}
		int index = filename.indexOf("'");
		if (index != -1) {
			Charset charset = null;
			try {
				charset = Charset.forName(filename.substring(0, index));
			}
			catch (IllegalArgumentException ex) {
				// ignore
			}
			filename = filename.substring(index + 1);
			// Skip language information..
			index = filename.indexOf("'");
			if (index != -1) {
				filename = filename.substring(index + 1);
			}
			if (charset != null) {
				filename = new String(filename.getBytes(US_ASCII), charset);
			}
		}
		return filename;
	}


	@Override
	protected void initializeMultipart() {
		parseRequest(getRequest());
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (this.multipartParameterNames == null) {
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			return super.getParameterNames();
		}

		// Servlet 3.0 getParameterNames() 不保证包含multipart表单项 (e.g. 在WebLogic 12上) -> 需要将它们合并到这里以保证安全
		Set<String> paramNames = new LinkedHashSet<String>();
		Enumeration<String> paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			paramNames.add(paramEnum.nextElement());
		}
		paramNames.addAll(this.multipartParameterNames);
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.multipartParameterNames == null) {
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			return super.getParameterMap();
		}

		// Servlet 3.0 getParameterMap() 不保证包含multipart表单项 (e.g. 在WebLogic 12上) -> 需要将它们合并到这里以保证安全
		Map<String, String[]> paramMap = new LinkedHashMap<String, String[]>();
		paramMap.putAll(super.getParameterMap());
		for (String paramName : this.multipartParameterNames) {
			if (!paramMap.containsKey(paramName)) {
				paramMap.put(paramName, getParameterValues(paramName));
			}
		}
		return paramMap;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		try {
			Part part = getPart(paramOrFileName);
			return (part != null ? part.getContentType() : null);
		}
		catch (Throwable ex) {
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		try {
			Part part = getPart(paramOrFileName);
			if (part != null) {
				HttpHeaders headers = new HttpHeaders();
				for (String headerName : part.getHeaderNames()) {
					headers.put(headerName, new ArrayList<String>(part.getHeaders(headerName)));
				}
				return headers;
			}
			else {
				return null;
			}
		}
		catch (Throwable ex) {
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}


	/**
	 * Spring MultipartFile适配器, 包装Servlet 3.0 Part对象.
	 */
	@SuppressWarnings("serial")
	private static class StandardMultipartFile implements MultipartFile, Serializable {

		private final Part part;

		private final String filename;

		public StandardMultipartFile(Part part, String filename) {
			this.part = part;
			this.filename = filename;
		}

		@Override
		public String getName() {
			return this.part.getName();
		}

		@Override
		public String getOriginalFilename() {
			return this.filename;
		}

		@Override
		public String getContentType() {
			return this.part.getContentType();
		}

		@Override
		public boolean isEmpty() {
			return (this.part.getSize() == 0);
		}

		@Override
		public long getSize() {
			return this.part.getSize();
		}

		@Override
		public byte[] getBytes() throws IOException {
			return FileCopyUtils.copyToByteArray(this.part.getInputStream());
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.part.getInputStream();
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			this.part.write(dest.getPath());
			if (dest.isAbsolute() && !dest.exists()) {
				// Servlet 3.0 Part.write不保证支持绝对文件路径:
				// 可以将给定路径转换为临时目录内的相对位置 (e.g. 在Jetty上, 而Tomcat和Undertow检测绝对路径).
				// 至少从内存存储中卸载了文件; 在任何情况下, 它最终都会从临时目录中删除.
				// 出于用户目的, 可以手动将其复制到所请求的位置作为后备.
				FileCopyUtils.copy(this.part.getInputStream(), new FileOutputStream(dest));
			}
		}
	}

}
