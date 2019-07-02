package org.springframework.mock.web.portlet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.portlet.multipart.MultipartActionRequest;

/**
 * {@link org.springframework.web.portlet.multipart.MultipartActionRequest}接口的模拟实现.
 *
 * <p>用于测试访问multipart上传的应用程序控制器.
 * {@link org.springframework.mock.web.MockMultipartFile}可用于使用文件填充这些模拟请求.
 */
public class MockMultipartActionRequest extends MockActionRequest implements MultipartActionRequest {

	private final MultiValueMap<String, MultipartFile> multipartFiles =
			new LinkedMultiValueMap<String, MultipartFile>();


	/**
	 * 将文件添加到此请求.
	 * multipart表单中的参数名称取自{@link org.springframework.web.multipart.MultipartFile#getName()}.
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

}
