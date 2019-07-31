package org.springframework.web.multipart;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.InputStreamSource;

/**
 * 在multipart请求中接收的上传文件的表示.
 *
 * <p>文件内容存储在内存中或临时存储在磁盘上.
 * 在任何一种情况下, 如果需要, 用户负责将文件内容复制到会话级或持久性存储.
 * 临时存储将在请求处理结束时清除.
 */
public interface MultipartFile extends InputStreamSource {

	/**
	 * 返回multipart表单中参数的名称.
	 * 
	 * @return 参数的名称 (never {@code null} or empty)
	 */
	String getName();

	/**
	 * 返回客户端的文件系统中的原始文件名.
	 * <p>这可能包含路径信息, 具体取决于所使用的浏览器, 但通常只有Opera会有.
	 * 
	 * @return 原始文件名, 如果在multipart表单中未选择任何文件, 则为空字符串; 如果未定义或不可用, 则为{@code null}
	 */
	String getOriginalFilename();

	/**
	 * 返回文件的内容类型.
	 * 
	 * @return 内容类型, 或{@code null} (如果未定义, 或者在multipart表单中未选择任何文件)
	 */
	String getContentType();

	/**
	 * 返回上传的文件是否为空, 即, 在multipart表单中没有选择任何文件, 或者所选文件没有内容.
	 */
	boolean isEmpty();

	/**
	 * 返回文件的大小, 以字节为单位.
	 * 
	 * @return 文件的大小, 或 0
	 */
	long getSize();

	/**
	 * 返回文件的内容.
	 * 
	 * @return 文件的内容, 或空字节数组
	 * @throws IOException 如果访问错误 (如果临时存储失败)
	 */
	byte[] getBytes() throws IOException;

	/**
	 * 返回从中读取文件的内容的InputStream.
	 * <p>用户负责关闭返回的流.
	 * 
	 * @return 文件的内容, 或空流
	 * @throws IOException 如果访问错误 (如果临时存储失败)
	 */
	@Override
	InputStream getInputStream() throws IOException;

	/**
	 * 将收到的文件传输到给定的目标文件.
	 * <p>这可以在文件系统中移动文件, 在文件系统中复制文件, 或将内存保存的内容保存到目标文件.
	 * 如果目标文件已存在, 则将首先删除它.
	 * <p>如果目标文件已在文件系统中移动, 则之后不能再次调用此操作.
	 * 因此, 只需调用此方法一次即可使用任何存储机制.
	 * <p><b>NOTE:</b> 根据底层提供者, 临时存储可能与容器有关, 包括此处指定的相对目标的基本目录 (e.g. 使用Servlet 3.0 multipart处理).
	 * 对于绝对目标, 即使临时副本已存在, 目标文件也可能从其临时位置重新命名/移动或新复制.
	 * 
	 * @param dest 目标文件 (通常是绝对的)
	 * 
	 * @throws IOException 在读或写错误的情况下
	 * @throws IllegalStateException 如果文件已经在文件系统中移动, 并且不再可用于其他传输
	 */
	void transferTo(File dest) throws IOException, IllegalStateException;

}
