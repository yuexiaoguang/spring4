package org.springframework.web.accept;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * 将{@link MediaType}解析为文件扩展名列表的策略.
 * 例如将"application/json"解析为"json".
 */
public interface MediaTypeFileExtensionResolver {

	/**
	 * 将给定的媒体类型解析为路径扩展列表.
	 * 
	 * @param mediaType 要解析的媒体类型
	 * 
	 * @return 扩展名列表或空列表 (never {@code null})
	 */
	List<String> resolveFileExtensions(MediaType mediaType);

	/**
	 * 返回所有注册的文件扩展名.
	 * 
	 * @return 扩展名列表或空列表 (never {@code null})
	 */
	List<String> getAllFileExtensions();

}
