package org.springframework.http.converter;

import java.io.IOException;
import java.io.InputStream;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * 使用JAF解析给定{@link Resource}的{@code MediaType}.
 */
class ActivationMediaTypeFactory {

	private static final FileTypeMap fileTypeMap;

	static {
		fileTypeMap = loadFileTypeMapFromContextSupportModule();
	}

	private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
		// 是否可以从context-support模块中找到扩展的 mime.types...
		Resource mappingLocation = new ClassPathResource("org/springframework/mail/javamail/mime.types");
		if (mappingLocation.exists()) {
			InputStream inputStream = null;
			try {
				inputStream = mappingLocation.getInputStream();
				return new MimetypesFileTypeMap(inputStream);
			}
			catch (IOException ex) {
				// ignore
			}
			finally {
				if (inputStream != null) {
					try {
						inputStream.close();
					}
					catch (IOException ex) {
						// ignore
					}
				}
			}
		}
		return FileTypeMap.getDefaultFileTypeMap();
	}

	public static MediaType getMediaType(Resource resource) {
		String filename = resource.getFilename();
		if (filename != null) {
			String mediaType = fileTypeMap.getContentType(filename);
			if (StringUtils.hasText(mediaType)) {
				return MediaType.parseMediaType(mediaType);
			}
		}
		return null;
	}
}
