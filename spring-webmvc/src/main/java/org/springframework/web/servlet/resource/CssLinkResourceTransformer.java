package org.springframework.web.servlet.resource;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * 一个{@link ResourceTransformer}实现, 它修改CSS文件中的链接以匹配应向客户端公开的公共URL路径 (e.g. 在URL中插入基于MD5内容的哈希).
 *
 * <p>该实现在CSS {@code @import}语句中以及CSS {@code url()}函数中查找链接.
 * 然后所有链接都通过{@link ResourceResolverChain}传递, 并相对于包含CSS文件的位置进行解析.
 * 如果成功解析, 则修改链接, 否则保留原始链接.
 */
public class CssLinkResourceTransformer extends ResourceTransformerSupport {

	private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	private static final Log logger = LogFactory.getLog(CssLinkResourceTransformer.class);

	private final List<CssLinkParser> linkParsers = new ArrayList<CssLinkParser>(2);


	public CssLinkResourceTransformer() {
		this.linkParsers.add(new ImportStatementCssLinkParser());
		this.linkParsers.add(new UrlFunctionCssLinkParser());
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		resource = transformerChain.transform(request, resource);

		String filename = resource.getFilename();
		if (!"css".equals(StringUtils.getFilenameExtension(filename))) {
			return resource;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Transforming resource: " + resource);
		}

		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		String content = new String(bytes, DEFAULT_CHARSET);

		Set<CssLinkInfo> infos = new HashSet<CssLinkInfo>(8);
		for (CssLinkParser parser : this.linkParsers) {
			parser.parseLink(content, infos);
		}

		if (infos.isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("No links found.");
			}
			return resource;
		}

		List<CssLinkInfo> sortedInfos = new ArrayList<CssLinkInfo>(infos);
		Collections.sort(sortedInfos);

		int index = 0;
		StringWriter writer = new StringWriter();
		for (CssLinkInfo info : sortedInfos) {
			writer.write(content.substring(index, info.getStart()));
			String link = content.substring(info.getStart(), info.getEnd());
			String newLink = null;
			if (!hasScheme(link)) {
				newLink = resolveUrlPath(link, request, resource, transformerChain);
			}
			if (logger.isTraceEnabled()) {
				if (newLink != null && !link.equals(newLink)) {
					logger.trace("Link modified: " + newLink + " (original: " + link + ")");
				}
				else {
					logger.trace("Link not modified: " + link);
				}
			}
			writer.write(newLink != null ? newLink : link);
			index = info.getEnd();
		}
		writer.write(content.substring(index));

		return new TransformedResource(resource, writer.toString().getBytes(DEFAULT_CHARSET));
	}

	private boolean hasScheme(String link) {
		int schemeIndex = link.indexOf(':');
		return (schemeIndex > 0 && !link.substring(0, schemeIndex).contains("/")) || link.indexOf("//") == 0;
	}


	protected interface CssLinkParser {

		void parseLink(String content, Set<CssLinkInfo> linkInfos);
	}


	protected static abstract class AbstractCssLinkParser implements CssLinkParser {

		/**
		 * 返回用于搜索链接的关键字.
		 */
		protected abstract String getKeyword();

		@Override
		public void parseLink(String content, Set<CssLinkInfo> linkInfos) {
			int index = 0;
			do {
				index = content.indexOf(getKeyword(), index);
				if (index == -1) {
					break;
				}
				index = skipWhitespace(content, index + getKeyword().length());
				if (content.charAt(index) == '\'') {
					index = addLink(index, "'", content, linkInfos);
				}
				else if (content.charAt(index) == '"') {
					index = addLink(index, "\"", content, linkInfos);
				}
				else {
					index = extractLink(index, content, linkInfos);

				}
			}
			while (true);
		}

		private int skipWhitespace(String content, int index) {
			while (true) {
				if (Character.isWhitespace(content.charAt(index))) {
					index++;
					continue;
				}
				return index;
			}
		}

		protected int addLink(int index, String endKey, String content, Set<CssLinkInfo> linkInfos) {
			int start = index + 1;
			int end = content.indexOf(endKey, start);
			linkInfos.add(new CssLinkInfo(start, end));
			return end + endKey.length();
		}

		/**
		 * 在删除空格后, 并且下一个char既不是单引号也不是双引号时, 在关键字匹配后调用.
		 */
		protected abstract int extractLink(int index, String content, Set<CssLinkInfo> linkInfos);

	}


	private static class ImportStatementCssLinkParser extends AbstractCssLinkParser {

		@Override
		protected String getKeyword() {
			return "@import";
		}

		@Override
		protected int extractLink(int index, String content, Set<CssLinkInfo> linkInfos) {
			if (content.substring(index, index + 4).equals("url(")) {
				// Ignore, UrlLinkParser will take care
			}
			else if (logger.isErrorEnabled()) {
				logger.error("Unexpected syntax for @import link at index " + index);
			}
			return index;
		}
	}


	private static class UrlFunctionCssLinkParser extends AbstractCssLinkParser {

		@Override
		protected String getKeyword() {
			return "url(";
		}

		@Override
		protected int extractLink(int index, String content, Set<CssLinkInfo> linkInfos) {
			// A url() function without unquoted
			return addLink(index - 1, ")", content, linkInfos);
		}
	}


	private static class CssLinkInfo implements Comparable<CssLinkInfo> {

		private final int start;

		private final int end;

		public CssLinkInfo(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public int getStart() {
			return this.start;
		}

		public int getEnd() {
			return this.end;
		}

		@Override
		public int compareTo(CssLinkInfo other) {
			return (this.start < other.start ? -1 : (this.start == other.start ? 0 : 1));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj != null && obj instanceof CssLinkInfo) {
				CssLinkInfo other = (CssLinkInfo) obj;
				return (this.start == other.start && this.end == other.end);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.start * 31 + this.end;
		}
	}

}
