package org.springframework.web.servlet.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ResourceTransformer}实现, 有助于处理HTML5 AppCache清单中的资源, 用于HTML5离线应用程序.
 *
 * <p>此转换器:
 * <ul>
 * <li>使用已配置的{@code ResourceResolver}策略修改链接以匹配应向客户端公开的公共URL路径
 * <li>在清单中附加注释, 包含哈希 (e.g. "# Hash: 9de0f09ed7caf84e885f1f0f11c7e326"),
 * 从而更改清单的内容以便在浏览器中触发appcache重新加载.
 * </ul>
 *
 * 具有".manifest"文件扩展名或构造函数中给出的扩展名的所有文件将由此类转换.
 *
 * <p>使用appcache清单的内容和链接资源的内容计算此哈希值; 因此, 更改清单中链接的资源或清单本身应使浏览器缓存无效.
 */
public class AppCacheManifestTransformer extends ResourceTransformerSupport {

	private static final String MANIFEST_HEADER = "CACHE MANIFEST";

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final Log logger = LogFactory.getLog(AppCacheManifestTransformer.class);


	private final Map<String, SectionTransformer> sectionTransformers = new HashMap<String, SectionTransformer>();

	private final String fileExtension;


	/**
	 * 用于转换扩展名为".manifest"的文件.
	 */
	public AppCacheManifestTransformer() {
		this("manifest");
	}

	/**
	 * 使用给定的扩展名转换文件.
	 */
	public AppCacheManifestTransformer(String fileExtension) {
		this.fileExtension = fileExtension;

		SectionTransformer noOpSection = new NoOpSection();
		this.sectionTransformers.put(MANIFEST_HEADER, noOpSection);
		this.sectionTransformers.put("NETWORK:", noOpSection);
		this.sectionTransformers.put("FALLBACK:", noOpSection);
		this.sectionTransformers.put("CACHE:", new CacheSection());
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		resource = transformerChain.transform(request, resource);
		if (!this.fileExtension.equals(StringUtils.getFilenameExtension(resource.getFilename()))) {
			return resource;
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		if (!content.startsWith(MANIFEST_HEADER)) {
			if (logger.isTraceEnabled()) {
				logger.trace("AppCache manifest does not start with 'CACHE MANIFEST', skipping: " + resource);
			}
			return resource;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Transforming resource: " + resource);
		}

		StringWriter contentWriter = new StringWriter();
		HashBuilder hashBuilder = new HashBuilder(content.length());

		Scanner scanner = new Scanner(content);
		SectionTransformer currentTransformer = this.sectionTransformers.get(MANIFEST_HEADER);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (this.sectionTransformers.containsKey(line.trim())) {
				currentTransformer = this.sectionTransformers.get(line.trim());
				contentWriter.write(line + "\n");
				hashBuilder.appendString(line);
			}
			else {
				contentWriter.write(
						currentTransformer.transform(line, hashBuilder, resource, transformerChain, request)  + "\n");
			}
		}

		String hash = hashBuilder.build();
		contentWriter.write("\n" + "# Hash: " + hash);
		if (logger.isTraceEnabled()) {
			logger.trace("AppCache file: [" + resource.getFilename()+ "] hash: [" + hash + "]");
		}

		return new TransformedResource(resource, contentWriter.toString().getBytes(DEFAULT_CHARSET));
	}


	private static interface SectionTransformer {

		/**
		 * 转换清单一部分中的一行.
		 * <p>实际转换取决于当前清单部分选择的转换策略 (CACHE, NETWORK, FALLBACK, etc).
		 */
		String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, HttpServletRequest request) throws IOException;
	}


	private static class NoOpSection implements SectionTransformer {

		public String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, HttpServletRequest request) throws IOException {

			builder.appendString(line);
			return line;
		}
	}


	private class CacheSection implements SectionTransformer {

		private static final String COMMENT_DIRECTIVE = "#";

		@Override
		public String transform(String line, HashBuilder builder, Resource resource,
				ResourceTransformerChain transformerChain, HttpServletRequest request) throws IOException {

			if (isLink(line) && !hasScheme(line)) {
				ResourceResolverChain resolverChain = transformerChain.getResolverChain();
				Resource appCacheResource =
						resolverChain.resolveResource(null, line, Collections.singletonList(resource));
				String path = resolveUrlPath(line, request, resource, transformerChain);
				builder.appendResource(appCacheResource);
				if (logger.isTraceEnabled()) {
					logger.trace("Link modified: " + path + " (original: " + line + ")");
				}
				return path;
			}
			builder.appendString(line);
			return line;
		}

		private boolean hasScheme(String link) {
			int schemeIndex = link.indexOf(':');
			return (link.startsWith("//") || (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")));
		}

		private boolean isLink(String line) {
			return (StringUtils.hasText(line) && !line.startsWith(COMMENT_DIRECTIVE));
		}
	}


	private static class HashBuilder {

		private final ByteArrayOutputStream baos;

		public HashBuilder(int initialSize) {
			this.baos = new ByteArrayOutputStream(initialSize);
		}

		public void appendResource(Resource resource) throws IOException {
			byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
			this.baos.write(DigestUtils.md5Digest(content));
		}

		public void appendString(String content) throws IOException {
			this.baos.write(content.getBytes(DEFAULT_CHARSET));
		}

		public String build() {
			return DigestUtils.md5DigestAsHex(this.baos.toByteArray());
		}
	}
}
