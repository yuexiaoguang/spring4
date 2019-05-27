package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.UsesJava7;
import org.springframework.util.Assert;

/**
 * {@code java.nio.file.Path}的编辑器, 直接填充Path属性, 而不是使用String属性作为桥.
 *
 * <p>基于{@link Paths#get(URI)}的解析算法, 检查已注册的NIO文件系统提供程序, 包括 "file:..."路径的默认文件系统.
 * 还支持Spring风格的URL表示法: 任何完全限定的标准URL和Spring的特殊 "classpath:"伪URL, 以及Spring的特定于上下文的相对文件路径.
 * 作为回退, 如果找不到现有的上下文相关资源, 将通过{@code Paths#get(String)}在文件系统中解析路径.
 */
@UsesJava7
public class PathEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用下面的默认ResourceEditor创建一个新的PathEditor.
	 */
	public PathEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用下面给定的ResourceEditor创建一个新的PathEditor.
	 * 
	 * @param resourceEditor 要使用的ResourceEditor
	 */
	public PathEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		boolean nioPathCandidate = !text.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
		if (nioPathCandidate && !text.startsWith("/")) {
			try {
				URI uri = new URI(text);
				if (uri.getScheme() != null) {
					nioPathCandidate = false;
					// 让我们通过Paths.get(URI)尝试NIO文件系统提供程序
					setValue(Paths.get(uri).normalize());
					return;
				}
			}
			catch (URISyntaxException ex) {
				// 不是有效的URI: 让我们尝试Spring资源位置.
			}
			catch (FileSystemNotFoundException ex) {
				// 未注册NIO的URI方案: 让我们通过Spring的资源机制尝试URL协议处理程序.
			}
		}

		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();
		if (resource == null) {
			setValue(null);
		}
		else if (!resource.exists() && nioPathCandidate) {
			setValue(Paths.get(text).normalize());
		}
		else {
			try {
				setValue(resource.getFile().toPath());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
			}
		}
	}

	@Override
	public String getAsText() {
		Path value = (Path) getValue();
		return (value != null ? value.toString() : "");
	}

}
