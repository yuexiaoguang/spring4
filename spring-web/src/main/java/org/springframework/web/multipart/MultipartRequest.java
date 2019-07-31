package org.springframework.web.multipart;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.util.MultiValueMap;

/**
 * 此接口定义为实际multipart请求公开的multipart请求访问操作.
 * 它由{@link MultipartHttpServletRequest}和Portlet
 * {@link org.springframework.web.portlet.multipart.MultipartActionRequest}扩展.
 */
public interface MultipartRequest {

	/**
	 * 返回此请求中包含的multipart文件的参数名称的String对象的{@link java.util.Iterator}.
	 * 这些是表单的字段名称 (与普通参数一样), 而不是原始文件名.
	 * 
	 * @return the names of the files
	 */
	Iterator<String> getFileNames();

	/**
	 * 返回内容以及此请求中上传文件的描述, 或{@code null}.
	 * 
	 * @param name multipart文件的参数名称
	 * 
	 * @return 以{@link MultipartFile}对象的形式上传的内容
	 */
	MultipartFile getFile(String name);

	/**
	 * 返回内容以及此请求中上传文件的描述, 或空列表.
	 * 
	 * @param name multipart文件的参数名称
	 * 
	 * @return 以{@link MultipartFile}列表的形式上传的内容
	 */
	List<MultipartFile> getFiles(String name);

	/**
	 * 返回此请求中包含的multipart文件的{@link java.util.Map}.
	 * 
	 * @return 参数名称作为键, 以及{@link MultipartFile}对象作为值
	 */
	Map<String, MultipartFile> getFileMap();

	/**
	 * 返回此请求中包含的multipart文件的{@link MultiValueMap}.
	 * 
	 * @return 参数名称作为键, 以及{@link MultipartFile}对象作为值
	 */
	MultiValueMap<String, MultipartFile> getMultiFileMap();

	/**
	 * 确定指定请求部分的内容类型.
	 * 
	 * @param paramOrFileName 部分的名称
	 * 
	 * @return 关联的内容类型, 或{@code null}
	 */
	String getMultipartContentType(String paramOrFileName);

}
