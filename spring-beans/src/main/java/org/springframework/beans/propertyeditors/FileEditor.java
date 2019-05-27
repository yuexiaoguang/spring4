package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@code java.io.File}的编辑器, 从Spring资源位置直接填充File属性.
 *
 * <p>支持Spring样式的URL表示法: 任何完全合格的标准URL ("file:", "http:", etc) 和Spring特殊的 "classpath:" pseudo-URL.
 *
 * <p><b>NOTE:</b> 在Spring 2.0中, 此编辑器的行为已更改.
 * 以前, 它直接从文件名创建了一个File实例.
 * 从Spring 2.0开始, 它将标准的Spring资源位置作为输入; 这与URLEditor和InputStreamEditor现在一致.
 *
 * <p><b>NOTE:</b> 在Spring 2.5中进行了以下修改.
 * 如果指定的文件名没有URL前缀或没有绝对路径, 那么我们尝试使用标准的Res​​ourceLoader语义来定位文件.
 * 如果找不到该文件, 则假定文件名引用相对文件位置, 创建File实例.
 */
public class FileEditor extends PropertyEditorSupport {

	private final ResourceEditor resourceEditor;


	/**
	 * 使用下面的默认ResourceEditor创建一个新的FileEditor.
	 */
	public FileEditor() {
		this.resourceEditor = new ResourceEditor();
	}

	/**
	 * 使用下面的给定ResourceEditor创建一个新的FileEditor.
	 * 
	 * @param resourceEditor 要使用的ResourceEditor
	 */
	public FileEditor(ResourceEditor resourceEditor) {
		Assert.notNull(resourceEditor, "ResourceEditor must not be null");
		this.resourceEditor = resourceEditor;
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (!StringUtils.hasText(text)) {
			setValue(null);
			return;
		}

		// 检查我们是否有一个没有 "file:" 前缀的绝对文件路径.
		// 为了向后兼容, 将这些视为直接文件路径.
		File file = null;
		if (!ResourceUtils.isUrl(text)) {
			file = new File(text);
			if (file.isAbsolute()) {
				setValue(file);
				return;
			}
		}

		// 继续进行标准资源位置解析.
		this.resourceEditor.setAsText(text);
		Resource resource = (Resource) this.resourceEditor.getValue();

		// 如果它是指向现有资源的URL或路径, 请按原样使用它.
		if (file == null || resource.exists()) {
			try {
				setValue(resource.getFile());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException(
						"Could not retrieve file for " + resource + ": " + ex.getMessage());
			}
		}
		else {
			// 设置相对文件引用并希望最好.
			setValue(file);
		}
	}

	@Override
	public String getAsText() {
		File value = (File) getValue();
		return (value != null ? value.getPath() : "");
	}

}
