package org.springframework.http.converter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.mail.internet.MimeUtility;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link HttpMessageConverter}的实现, 以读取和写入'普通' HTML表单以及写入 (但不读取) multipart数据 (e.g. 文件上传).
 *
 * <p>换句话说, 这个转换器可以读取和写入
 * {@code "application/x-www-form-urlencoded"}媒体类型为 {@link MultiValueMap MultiValueMap&lt;String, String&gt;},
 * 它也可以写入 (但不能读取) {@code "multipart/form-data"}媒体类型为{@link MultiValueMap MultiValueMap&lt;String, Object&gt;}.
 *
 * <p>在写入multipart数据时, 此转换器使用其他{@link HttpMessageConverter HttpMessageConverters}来写入相应的MIME部分.
 * 默认情况下, 基本转换器已注册 (对于{@code Strings}和{@code Resources}).
 * 这些可以通过{@link #setPartConverters partConverters}属性覆盖.
 *
 * <p>例如, 以下代码段显示了如何提交HTML表单:
 * <pre class="code">
 * RestTemplate template = new RestTemplate();
 * // AllEncompassingFormHttpMessageConverter is configured by default
 *
 * MultiValueMap&lt;String, String&gt; form = new LinkedMultiValueMap&lt;&gt;();
 * form.add("field 1", "value 1");
 * form.add("field 2", "value 2");
 * form.add("field 2", "value 3");
 * template.postForLocation("http://example.com/myForm", form);
 * </pre>
 *
 * <p>以下代码段显示了如何进行文件上传:
 * <pre class="code">
 * MultiValueMap&lt;String, Object&gt; parts = new LinkedMultiValueMap&lt;&gt;();
 * parts.add("field 1", "value 1");
 * parts.add("file", new ClassPathResource("myFile.jpg"));
 * template.postForLocation("http://example.com/myFileUpload", parts);
 * </pre>
 *
 * <p>此类中的一些方法受到
 * {@code org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity}的启发.
 */
public class FormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

	public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");


	private List<MediaType> supportedMediaTypes = new ArrayList<MediaType>();

	private List<HttpMessageConverter<?>> partConverters = new ArrayList<HttpMessageConverter<?>>();

	private Charset charset = DEFAULT_CHARSET;

	private Charset multipartCharset;


	public FormHttpMessageConverter() {
		this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
		this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);

		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false);  // see SPR-7316

		this.partConverters.add(new ByteArrayHttpMessageConverter());
		this.partConverters.add(stringHttpMessageConverter);
		this.partConverters.add(new ResourceHttpMessageConverter());

		applyDefaultCharset();
	}


	/**
	 * 设置此转换器支持的{@link MediaType}对象列表.
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.supportedMediaTypes);
	}

	/**
	 * 设置要使用的消息正文转换器. 这些转换器用于将对象转换为MIME部分.
	 */
	public void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
		Assert.notEmpty(partConverters, "'partConverters' must not be empty");
		this.partConverters = partConverters;
	}

	/**
	 * 添加消息正文转换器. 这种转换器用于将对象转换为MIME部分.
	 */
	public void addPartConverter(HttpMessageConverter<?> partConverter) {
		Assert.notNull(partConverter, "'partConverter' must not be null");
		this.partConverters.add(partConverter);
	}

	/**
	 * 当请求或响应Content-Type header未明确指定时, 设置用于读取和写入表单数据的默认字符集.
	 * <p>默认 "UTF-8". 从4.3开始, 它还将用作多部分请求中文本正文转换的默认字符集.
	 * 与此相反, {@link #setMultipartCharset}仅根据编码字语法影响多部分请求中<i>文件名</i>的编码.
	 */
	public void setCharset(Charset charset) {
		if (charset != this.charset) {
			this.charset = (charset != null ? charset : DEFAULT_CHARSET);
			applyDefaultCharset();
		}
	}

	/**
	 * 将配置的字符集作为默认值应用于已注册的部件转换器.
	 */
	private void applyDefaultCharset() {
		for (HttpMessageConverter<?> candidate : this.partConverters) {
			if (candidate instanceof AbstractHttpMessageConverter) {
				AbstractHttpMessageConverter<?> converter = (AbstractHttpMessageConverter<?>) candidate;
				// 如果转换器以charset开始运行, 则仅覆盖默认字符集...
				if (converter.getDefaultCharset() != null) {
					converter.setDefaultCharset(this.charset);
				}
			}
		}
	}

	/**
	 * 设置在写入多部分数据时使用的字符集, 以编码文件名.
	 * 编码基于RFC 2047中定义的编码字语法，并依赖于来自"javax.mail"的{@code MimeUtility}.
	 * <p>如果未设置, 文件名将被编码为US-ASCII.
	 */
	public void setMultipartCharset(Charset charset) {
		this.multipartCharset = charset;
	}


	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			// We can't read multipart....
			if (!supportedMediaType.equals(MediaType.MULTIPART_FORM_DATA) && supportedMediaType.includes(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		if (!MultiValueMap.class.isAssignableFrom(clazz)) {
			return false;
		}
		if (mediaType == null || MediaType.ALL.equals(mediaType)) {
			return true;
		}
		for (MediaType supportedMediaType : getSupportedMediaTypes()) {
			if (supportedMediaType.isCompatibleWith(mediaType)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public MultiValueMap<String, String> read(Class<? extends MultiValueMap<String, ?>> clazz,
			HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {

		MediaType contentType = inputMessage.getHeaders().getContentType();
		Charset charset = (contentType.getCharset() != null ? contentType.getCharset() : this.charset);
		String body = StreamUtils.copyToString(inputMessage.getBody(), charset);

		String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
		MultiValueMap<String, String> result = new LinkedMultiValueMap<String, String>(pairs.length);
		for (String pair : pairs) {
			int idx = pair.indexOf('=');
			if (idx == -1) {
				result.add(URLDecoder.decode(pair, charset.name()), null);
			}
			else {
				String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
				String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
				result.add(name, value);
			}
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(MultiValueMap<String, ?> map, MediaType contentType, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		if (!isMultipart(map, contentType)) {
			writeForm((MultiValueMap<String, String>) map, contentType, outputMessage);
		}
		else {
			writeMultipart((MultiValueMap<String, Object>) map, outputMessage);
		}
	}


	private boolean isMultipart(MultiValueMap<String, ?> map, MediaType contentType) {
		if (contentType != null) {
			return MediaType.MULTIPART_FORM_DATA.includes(contentType);
		}
		for (String name : map.keySet()) {
			for (Object value : map.get(name)) {
				if (value != null && !(value instanceof String)) {
					return true;
				}
			}
		}
		return false;
	}

	private void writeForm(MultiValueMap<String, String> form, MediaType contentType,
			HttpOutputMessage outputMessage) throws IOException {

		Charset charset;
		if (contentType != null) {
			outputMessage.getHeaders().setContentType(contentType);
			charset = (contentType.getCharset() != null ? contentType.getCharset() : this.charset);
		}
		else {
			outputMessage.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			charset = this.charset;
		}
		StringBuilder builder = new StringBuilder();
		for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext();) {
			String name = nameIterator.next();
			for (Iterator<String> valueIterator = form.get(name).iterator(); valueIterator.hasNext();) {
				String value = valueIterator.next();
				builder.append(URLEncoder.encode(name, charset.name()));
				if (value != null) {
					builder.append('=');
					builder.append(URLEncoder.encode(value, charset.name()));
					if (valueIterator.hasNext()) {
						builder.append('&');
					}
				}
			}
			if (nameIterator.hasNext()) {
				builder.append('&');
			}
		}
		final byte[] bytes = builder.toString().getBytes(charset.name());
		outputMessage.getHeaders().setContentLength(bytes.length);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					StreamUtils.copy(bytes, outputStream);
				}
			});
		}
		else {
			StreamUtils.copy(bytes, outputMessage.getBody());
		}
	}

	private void writeMultipart(final MultiValueMap<String, Object> parts, HttpOutputMessage outputMessage) throws IOException {
		final byte[] boundary = generateMultipartBoundary();
		Map<String, String> parameters = Collections.singletonMap("boundary", new String(boundary, "US-ASCII"));

		MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
		HttpHeaders headers = outputMessage.getHeaders();
		headers.setContentType(contentType);

		if (outputMessage instanceof StreamingHttpOutputMessage) {
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			streamingOutputMessage.setBody(new StreamingHttpOutputMessage.Body() {
				@Override
				public void writeTo(OutputStream outputStream) throws IOException {
					writeParts(outputStream, parts, boundary);
					writeEnd(outputStream, boundary);
				}
			});
		}
		else {
			writeParts(outputMessage.getBody(), parts, boundary);
			writeEnd(outputMessage.getBody(), boundary);
		}
	}

	private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
		for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
			String name = entry.getKey();
			for (Object part : entry.getValue()) {
				if (part != null) {
					writeBoundary(os, boundary);
					writePart(name, getHttpEntity(part), os);
					writeNewLine(os);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
		Object partBody = partEntity.getBody();
		Class<?> partType = partBody.getClass();
		HttpHeaders partHeaders = partEntity.getHeaders();
		MediaType partContentType = partHeaders.getContentType();
		for (HttpMessageConverter<?> messageConverter : this.partConverters) {
			if (messageConverter.canWrite(partType, partContentType)) {
				HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os);
				multipartMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
				if (!partHeaders.isEmpty()) {
					multipartMessage.getHeaders().putAll(partHeaders);
				}
				((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
				return;
			}
		}
		throw new HttpMessageNotWritableException("Could not write request: no suitable HttpMessageConverter " +
				"found for request type [" + partType.getName() + "]");
	}


	/**
	 * 生成多部分边界.
	 * <p>此实现委托给{@link MimeTypeUtils#generateMultipartBoundary()}.
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * 为给定的部分Object返回{@link HttpEntity}.
	 * 
	 * @param part 返回{@link HttpEntity}的部分
	 * 
	 * @return 部分对象本身是{@link HttpEntity}, 或者是新构建的{@link HttpEntity}包装器
	 */
	protected HttpEntity<?> getHttpEntity(Object part) {
		return (part instanceof HttpEntity ? (HttpEntity<?>) part : new HttpEntity<Object>(part));
	}

	/**
	 * 返回给定多部分的文件名. 此值将用于{@code Content-Disposition} header.
	 * <p>如果part是{@code Resource}, 则默认实现返回{@link Resource#getFilename()}, 而在其他情况下 {@code null}.
	 * 可以在子类中重写.
	 * 
	 * @param part 确定文件名的部分
	 * 
	 * @return 文件名, 或{@code null}
	 */
	protected String getFilename(Object part) {
		if (part instanceof Resource) {
			Resource resource = (Resource) part;
			String filename = resource.getFilename();
			if (filename != null && this.multipartCharset != null) {
				filename = MimeDelegate.encode(filename, this.multipartCharset.name());
			}
			return filename;
		}
		else {
			return null;
		}
	}


	private void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		os.write('-');
		os.write('-');
		writeNewLine(os);
	}

	private static void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}


	/**
	 * 用于写入MIME多部分的{@link org.springframework.http.HttpOutputMessage}的实现.
	 */
	private static class MultipartHttpOutputMessage implements HttpOutputMessage {

		private final OutputStream outputStream;

		private final HttpHeaders headers = new HttpHeaders();

		private boolean headersWritten = false;

		public MultipartHttpOutputMessage(OutputStream outputStream) {
			this.outputStream = outputStream;
		}

		@Override
		public HttpHeaders getHeaders() {
			return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
		}

		@Override
		public OutputStream getBody() throws IOException {
			writeHeaders();
			return this.outputStream;
		}

		private void writeHeaders() throws IOException {
			if (!this.headersWritten) {
				for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
					byte[] headerName = getAsciiBytes(entry.getKey());
					for (String headerValueString : entry.getValue()) {
						byte[] headerValue = getAsciiBytes(headerValueString);
						this.outputStream.write(headerName);
						this.outputStream.write(':');
						this.outputStream.write(' ');
						this.outputStream.write(headerValue);
						writeNewLine(this.outputStream);
					}
				}
				writeNewLine(this.outputStream);
				this.headersWritten = true;
			}
		}

		private byte[] getAsciiBytes(String name) {
			try {
				return name.getBytes("US-ASCII");
			}
			catch (UnsupportedEncodingException ex) {
				// Should not happen - US-ASCII is always supported.
				throw new IllegalStateException(ex);
			}
		}
	}


	/**
	 * 内部类, 以避免对JavaMail API的硬依赖.
	 */
	private static class MimeDelegate {

		public static String encode(String value, String charset) {
			try {
				return MimeUtility.encodeText(value, charset, null);
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}

}
