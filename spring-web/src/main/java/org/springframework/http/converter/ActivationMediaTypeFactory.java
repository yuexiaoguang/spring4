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
 * Resolve {@code MediaType} for a given {@link Resource} using JAF.
 */
class ActivationMediaTypeFactory {

	private static final FileTypeMap fileTypeMap;

	static {
		fileTypeMap = loadFileTypeMapFromContextSupportModule();
	}

	private static FileTypeMap loadFileTypeMapFromContextSupportModule() {
		// See if we can find the extended mime.types from the context-support module...
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
