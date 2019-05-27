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
 * Mock implementation of the
 * {@link org.springframework.web.portlet.multipart.MultipartActionRequest} interface.
 *
 * <p>Useful for testing application controllers that access multipart uploads.
 * The {@link org.springframework.mock.web.MockMultipartFile} can be used to
 * populate these mock requests with files.
 */
public class MockMultipartActionRequest extends MockActionRequest implements MultipartActionRequest {

	private final MultiValueMap<String, MultipartFile> multipartFiles =
			new LinkedMultiValueMap<String, MultipartFile>();


	/**
	 * Add a file to this request. The parameter name from the multipart
	 * form is taken from the {@link org.springframework.web.multipart.MultipartFile#getName()}.
	 * @param file multipart file to be added
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
