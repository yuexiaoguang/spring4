package org.springframework.mock.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * {@link org.springframework.web.multipart.MultipartFile}接口的模拟实现.
 *
 * <p>与{@link MockMultipartHttpServletRequest}结合使用, 用于测试访问multipart上传的应用程序控制器.
 */
public class MockMultipartFile implements MultipartFile {

	private final String name;

	private String originalFilename;

	private String contentType;

	private final byte[] content;


	/**
	 * @param name 文件名
	 * @param content 文件的内容
	 */
	public MockMultipartFile(String name, byte[] content) {
		this(name, "", null, content);
	}

	/**
	 * @param name 文件名
	 * @param contentStream 文件的内容
	 * 
	 * @throws IOException 如果从流中读取失败
	 */
	public MockMultipartFile(String name, InputStream contentStream) throws IOException {
		this(name, "", null, FileCopyUtils.copyToByteArray(contentStream));
	}

	/**
	 * @param name 文件名
	 * @param originalFilename 原始文件名 (如在客户端的机器上)
	 * @param contentType 内容类型 (如果已知)
	 * @param content 文件的内容
	 */
	public MockMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
		Assert.hasLength(name, "Name must not be null");
		this.name = name;
		this.originalFilename = (originalFilename != null ? originalFilename : "");
		this.contentType = contentType;
		this.content = (content != null ? content : new byte[0]);
	}

	/**
	 * @param name 文件名
	 * @param originalFilename 原始文件名 (如在客户端的机器上)
	 * @param contentType 内容类型 (如果已知)
	 * @param contentStream 文件的内容
	 * 
	 * @throws IOException 如果从流中读取失败
	 */
	public MockMultipartFile(String name, String originalFilename, String contentType, InputStream contentStream)
			throws IOException {

		this(name, originalFilename, contentType, FileCopyUtils.copyToByteArray(contentStream));
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getOriginalFilename() {
		return this.originalFilename;
	}

	@Override
	public String getContentType() {
		return this.contentType;
	}

	@Override
	public boolean isEmpty() {
		return (this.content.length == 0);
	}

	@Override
	public long getSize() {
		return this.content.length;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return this.content;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return new ByteArrayInputStream(this.content);
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		FileCopyUtils.copy(this.content, dest);
	}

}
