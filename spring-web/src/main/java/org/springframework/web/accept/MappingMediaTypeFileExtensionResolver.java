package org.springframework.web.accept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * An implementation of {@code MediaTypeFileExtensionResolver} that maintains
 * lookups between file extensions and MediaTypes in both directions.
 *
 * <p>Initially created with a map of file extensions and media types.
 * Subsequently subclasses can use {@link #addMapping} to add more mappings.
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

	private final ConcurrentMap<String, MediaType> mediaTypes =
			new ConcurrentHashMap<String, MediaType>(64);

	private final MultiValueMap<MediaType, String> fileExtensions =
			new LinkedMultiValueMap<MediaType, String>();

	private final List<String> allFileExtensions = new LinkedList<String>();


	/**
	 * Create an instance with the given map of file extensions and media types.
	 */
	public MappingMediaTypeFileExtensionResolver(Map<String, MediaType> mediaTypes) {
		if (mediaTypes != null) {
			for (Map.Entry<String, MediaType> entries : mediaTypes.entrySet()) {
				String extension = entries.getKey().toLowerCase(Locale.ENGLISH);
				MediaType mediaType = entries.getValue();
				this.mediaTypes.put(extension, mediaType);
				this.fileExtensions.add(mediaType, extension);
				this.allFileExtensions.add(extension);
			}
		}
	}


	public Map<String, MediaType> getMediaTypes() {
		return this.mediaTypes;
	}

	protected List<MediaType> getAllMediaTypes() {
		return new ArrayList<MediaType>(this.mediaTypes.values());
	}

	/**
	 * Map an extension to a MediaType. Ignore if extension already mapped.
	 */
	protected void addMapping(String extension, MediaType mediaType) {
		MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
		if (previous == null) {
			this.fileExtensions.add(mediaType, extension);
			this.allFileExtensions.add(extension);
		}
	}


	@Override
	public List<String> resolveFileExtensions(MediaType mediaType) {
		List<String> fileExtensions = this.fileExtensions.get(mediaType);
		return (fileExtensions != null) ? fileExtensions : Collections.<String>emptyList();
	}

	@Override
	public List<String> getAllFileExtensions() {
		return Collections.unmodifiableList(this.allFileExtensions);
	}

	/**
	 * Use this method for a reverse lookup from extension to MediaType.
	 * @return a MediaType for the key, or {@code null} if none found
	 */
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
	}

}
