package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * {@link PropertiesPersister}接口的默认实现.
 * 遵循{@code java.util.Properties}的原生解析.
 *
 * <p>允许从任何Reader读取并写入任何Writer, 例如为属性文件指定charset.
 * 这是一种标准{@code java.util.Properties}不幸缺少JDK 5的功能:
 * 只能使用ISO-8859-1 charset加载文件.
 *
 * <p>从流加载并存储到流, 分别委托给{@code Properties.load}和{@code Properties.store},
 * 以完全兼容JDK Properties类实现的Unicode转换.
 * 从JDK 6开始, {@code Properties.load/store}也将用于 readers/writer, 有效地将此类转换为普通的向后兼容适配器.
 *
 * <p>与Reader/Writer一起使用的持久性代码遵循JDK的解析策略, 但不实现Unicode转换,
 * 因为Reader/Writer应该已经对字符应用了正确的解码/编码.
 * 如果要在属性文件中转义unicode字符, <i>不要</i>指定Reader/Writer的编码
 * (例如 ReloadableResourceBundleMessageSource的"defaultEncoding" 和 "fileEncodings"属性).
 */
public class DefaultPropertiesPersister implements PropertiesPersister {

	@Override
	public void load(Properties props, InputStream is) throws IOException {
		props.load(is);
	}

	@Override
	public void load(Properties props, Reader reader) throws IOException {
		props.load(reader);
	}

	@Override
	public void store(Properties props, OutputStream os, String header) throws IOException {
		props.store(os, header);
	}

	@Override
	public void store(Properties props, Writer writer, String header) throws IOException {
		props.store(writer, header);
	}

	@Override
	public void loadFromXml(Properties props, InputStream is) throws IOException {
		props.loadFromXML(is);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header) throws IOException {
		props.storeToXML(os, header);
	}

	@Override
	public void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException {
		props.storeToXML(os, header, encoding);
	}

}
