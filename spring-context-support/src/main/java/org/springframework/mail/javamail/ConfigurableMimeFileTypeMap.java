package org.springframework.mail.javamail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.activation.FileTypeMap;
import javax.activation.MimetypesFileTypeMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Spring可配置的{@code FileTypeMap}实现, 它将从标准Ja​​vaMail MIME类型映射文件中读取MIME类型到文件扩展名映射,
 * 使用下面的标准{@code MimetypesFileTypeMap}.
 *
 * <p>映射文件应采用以下格式, 如Java Activation Framework所指定:
 *
 * <pre class="code">
 * # map text/html to .htm and .html files
 * text/html  html htm HTML HTM</pre>
 *
 * 以{@code #}开头的行被视为注释并被忽略.
 * 所有其他行都被视为映射.
 * 每个映射行应包含MIME类型作为第一个条目, 然后每个文件扩展名作为后续条目映射到该MIME类型.
 * 每个条目由空格或制表符分隔.
 *
 * <p>默认情况下, 使用位于与此类相同的包中的{@code mime.types}文件中的映射, 其中包含许多常见的文件扩展名
 * (与{@code activation.jar}中的开箱即用映射形成对比).
 * 可以使用{@code mappingLocation}属性覆盖此属性.
 *
 * <p>可以通过{@code mappings} bean属性添加其他映射, 作为遵循{@code mime.types}文件格式的行.
 */
public class ConfigurableMimeFileTypeMap extends FileTypeMap implements InitializingBean {

	/**
	 * 从中加载映射文件的{@code Resource}.
	 */
	private Resource mappingLocation = new ClassPathResource("mime.types", getClass());

	/**
	 * 用于配置其他映射.
	 */
	private String[] mappings;

	/**
	 * 委托FileTypeMap, 根据映射文件中的映射和{@code mappings}属性中的条目编译.
	 */
	private FileTypeMap fileTypeMap;


	/**
	 * 指定从中加载映射的{@code Resource}.
	 * <p>需要遵循Java激活框架所指定的{@code mime.types}文件格式, 包含以下行:<br>
	 * {@code text/html  html htm HTML HTM}
	 */
	public void setMappingLocation(Resource mappingLocation) {
		this.mappingLocation = mappingLocation;
	}

	/**
	 * 根据Java激活框架指定的{@code mime.types}文件格式指定其他MIME类型映射, 例如:<br>
	 * {@code text/html  html htm HTML HTM}
	 */
	public void setMappings(String... mappings) {
		this.mappings = mappings;
	}


	/**
	 * 创建最终合并的映射集合.
	 */
	@Override
	public void afterPropertiesSet() {
		getFileTypeMap();
	}

	/**
	 * 返回委托FileTypeMap, 从映射文件中的映射和{@code mappings}属性中的条目编译.
	 */
	protected final FileTypeMap getFileTypeMap() {
		if (this.fileTypeMap == null) {
			try {
				this.fileTypeMap = createFileTypeMap(this.mappingLocation, this.mappings);
			}
			catch (IOException ex) {
				throw new IllegalStateException(
						"Could not load specified MIME type mapping file: " + this.mappingLocation, ex);
			}
		}
		return this.fileTypeMap;
	}

	/**
	 * 从给定映射文件中的映射和给定的映射条目编译{@link FileTypeMap}.
	 * <p>默认实现创建一个Activation Framework {@link MimetypesFileTypeMap}, 从映射资源传入InputStream并以编程方式注册映射行.
	 * 
	 * @param mappingLocation {@code mime.types}映射资源 (can be {@code null})
	 * @param mappings MIME类型映射行 (can be {@code null})
	 * 
	 * @return 编译后的FileTypeMap
	 * @throws IOException 如果资源访问失败
	 */
	protected FileTypeMap createFileTypeMap(Resource mappingLocation, String[] mappings) throws IOException {
		MimetypesFileTypeMap fileTypeMap = null;
		if (mappingLocation != null) {
			InputStream is = mappingLocation.getInputStream();
			try {
				fileTypeMap = new MimetypesFileTypeMap(is);
			}
			finally {
				is.close();
			}
		}
		else {
			fileTypeMap = new MimetypesFileTypeMap();
		}
		if (mappings != null) {
			for (String mapping : mappings) {
				fileTypeMap.addMimeTypes(mapping);
			}
		}
		return fileTypeMap;
	}


	/**
	 * 委托给底层FileTypeMap.
	 */
	@Override
	public String getContentType(File file) {
		return getFileTypeMap().getContentType(file);
	}

	/**
	 * 委托给底层FileTypeMap.
	 */
	@Override
	public String getContentType(String fileName) {
		return getFileTypeMap().getContentType(fileName);
	}
}
