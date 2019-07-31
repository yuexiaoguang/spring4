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
 * {@code MediaTypeFileExtensionResolver}的实现, 用于维护文件扩展名和MediaTypes在两个方向上的查找.
 *
 * <p>使用文件扩展名和媒体类型的Map创建.
 * 随后的子类可以使用{@link #addMapping}添加更多映射.
 */
public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

	private final ConcurrentMap<String, MediaType> mediaTypes =
			new ConcurrentHashMap<String, MediaType>(64);

	private final MultiValueMap<MediaType, String> fileExtensions =
			new LinkedMultiValueMap<MediaType, String>();

	private final List<String> allFileExtensions = new LinkedList<String>();


	/**
	 * 使用给定的文件扩展名和媒体类型Map创建实例.
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
	 * 将扩展名映射到MediaType. 如果扩展名已映射, 则忽略.
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
	 * 使用此方法进行从扩展名到MediaType的反向查找.
	 * 
	 * @return 对应的MediaType, 或{@code null}
	 */
	protected MediaType lookupMediaType(String extension) {
		return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
	}

}
