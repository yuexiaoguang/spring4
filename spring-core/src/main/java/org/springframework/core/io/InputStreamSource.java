package org.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 作为{@link InputStream}源的对象的简单接口.
 *
 * <p>这是Spring更广泛的{@link Resource}接口的基本接口.
 *
 * <p>对于一次性使用的流, {@link InputStreamResource}可用于任何给定的{@code InputStream}.
 * Spring的{@link ByteArrayResource}或任何基于文件的{@code Resource}实现都可以用作具体实例, 允许多次读取底层内容流.
 * 例如, 这使得此接口可用作邮件附件的抽象内容源.
 */
public interface InputStreamSource {

	/**
	 * 返回用于获取底层资源的内容的{@link InputStream}.
	 * <p>预计每次调用都会创建一个<i>fresh</i>流.
	 * <p>当您考虑诸如JavaMail之类的API时, 此要求尤其重要, 该API需要能够在创建邮件附件时多次读取流.
	 * 对于这样的用例, <i>必需</i>每个{@code getInputStream()}调用返回一个新流.
	 * 
	 * @return 底层资源的输入流 (不能是 {@code null})
	 * @throws java.io.FileNotFoundException 如果底层资源不存在
	 * @throws IOException 如果无法打开内容流
	 */
	InputStream getInputStream() throws IOException;

}
