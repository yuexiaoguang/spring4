package org.springframework.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 表示与HTTP {@code "Range"} header一起使用的HTTP (字节)范围.
 */
public abstract class HttpRange {

	private static final String BYTE_RANGE_PREFIX = "bytes=";


	/**
	 * 使用当前{@code HttpRange}中包含的范围信息将{@code Resource}转换为{@link ResourceRegion}.
	 * 
	 * @param resource 从中选择区域的{@code Resource}
	 * 
	 * @return 给定{@code Resource}的选定区域
	 */
	public ResourceRegion toResourceRegion(Resource resource) {
		// 不要尝试在InputStreamResource上确定contentLength - 之后无法读取...
		// Note: 自定义InputStreamResource子类可以提供预先计算的内容长度!
		Assert.isTrue(resource.getClass() != InputStreamResource.class,
				"Cannot convert an InputStreamResource to a ResourceRegion");
		try {
			long contentLength = resource.contentLength();
			Assert.isTrue(contentLength > 0, "Resource content length should be > 0");
			long start = getRangeStart(contentLength);
			long end = getRangeEnd(contentLength);
			return new ResourceRegion(resource, start, end - start + 1);
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("Failed to convert Resource to ResourceRegion", ex);
		}
	}

	/**
	 * 给定总长度, 返回范围的开始.
	 * 
	 * @param length 表示的长度
	 * 
	 * @return 表示范围的开始
	 */
	public abstract long getRangeStart(long length);

	/**
	 * 给定表示的总长度, 返回范围的结尾 (包括).
	 * 
	 * @param length 表示的长度
	 * 
	 * @return 表示范围的结尾
	 */
	public abstract long getRangeEnd(long length);


	/**
	 * 从给定位置到结尾创建一个{@code HttpRange}.
	 * 
	 * @param firstBytePos 第一个字节位置
	 * 
	 * @return 从{@code firstPos}到结尾的字节范围
	 */
	public static HttpRange createByteRange(long firstBytePos) {
		return new ByteRange(firstBytePos, null);
	}

	/**
	 * 从给定的第一个到最后位置创建一个{@code HttpRange}.
	 * 
	 * @param firstBytePos 第一个字节位置
	 * @param lastBytePos 最后一个字节位置
	 * 
	 * @return 从{@code firstPos}到{@code lastPos}的字节范围
	 */
	public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
		return new ByteRange(firstBytePos, lastBytePos);
	}

	/**
	 * 创建一个范围超过最后给定字节数的{@code HttpRange}.
	 * 
	 * @param suffixLength 范围的字节数
	 * 
	 * @return 一个字节范围, 其范围超过最后{@code suffixLength}个字节数
	 */
	public static HttpRange createSuffixRange(long suffixLength) {
		return new SuffixByteRange(suffixLength);
	}

	/**
	 * 将给定的逗号分隔的字符串解析为{@code HttpRange}对象列表.
	 * <p>此方法可用于解析{@code Range} header.
	 * 
	 * @param ranges 要解析的字符串
	 * 
	 * @return 范围集合
	 * @throws IllegalArgumentException 如果字符串无法解析
	 */
	public static List<HttpRange> parseRanges(String ranges) {
		if (!StringUtils.hasLength(ranges)) {
			return Collections.emptyList();
		}
		if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
			throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
		}
		ranges = ranges.substring(BYTE_RANGE_PREFIX.length());

		String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");
		List<HttpRange> result = new ArrayList<HttpRange>(tokens.length);
		for (String token : tokens) {
			result.add(parseRange(token));
		}
		return result;
	}

	private static HttpRange parseRange(String range) {
		Assert.hasLength(range, "Range String must not be empty");
		int dashIdx = range.indexOf('-');
		if (dashIdx > 0) {
			long firstPos = Long.parseLong(range.substring(0, dashIdx));
			if (dashIdx < range.length() - 1) {
				Long lastPos = Long.parseLong(range.substring(dashIdx + 1, range.length()));
				return new ByteRange(firstPos, lastPos);
			}
			else {
				return new ByteRange(firstPos, null);
			}
		}
		else if (dashIdx == 0) {
			long suffixLength = Long.parseLong(range.substring(1));
			return new SuffixByteRange(suffixLength);
		}
		else {
			throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
		}
	}

	/**
	 * 将每个{@code HttpRange}转换为{@code ResourceRegion}, 使用HTTP范围信息选择给定{@code Resource}的相应片段.
	 * 
	 * @param ranges 范围集合
	 * @param resource 从中选择范围的资源
	 * 
	 * @return 给定资源的范围列表
	 */
	public static List<ResourceRegion> toResourceRegions(List<HttpRange> ranges, Resource resource) {
		if (CollectionUtils.isEmpty(ranges)) {
			return Collections.emptyList();
		}
		List<ResourceRegion> regions = new ArrayList<ResourceRegion>(ranges.size());
		for (HttpRange range : ranges) {
			regions.add(range.toResourceRegion(resource));
		}
		return regions;
	}

	/**
	 * 返回给定{@code HttpRange}对象列表的字符串表示形式.
	 * <p>此方法可用于{@code Range} header.
	 * 
	 * @param ranges 要创建字符串的范围
	 * 
	 * @return 字符串表示
	 */
	public static String toString(Collection<HttpRange> ranges) {
		Assert.notEmpty(ranges, "Ranges Collection must not be empty");
		StringBuilder builder = new StringBuilder(BYTE_RANGE_PREFIX);
		for (Iterator<HttpRange> iterator = ranges.iterator(); iterator.hasNext(); ) {
			HttpRange range = iterator.next();
			builder.append(range);
			if (iterator.hasNext()) {
				builder.append(", ");
			}
		}
		return builder.toString();
	}


	/**
	 * 表示HTTP/1.1 字节范围, 具有第一个和可选的最后位置.
	 */
	private static class ByteRange extends HttpRange {

		private final long firstPos;

		private final Long lastPos;

		public ByteRange(long firstPos, Long lastPos) {
			assertPositions(firstPos, lastPos);
			this.firstPos = firstPos;
			this.lastPos = lastPos;
		}

		private void assertPositions(long firstBytePos, Long lastBytePos) {
			if (firstBytePos < 0) {
				throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
			}
			if (lastBytePos != null && lastBytePos < firstBytePos) {
				throw new IllegalArgumentException("firstBytePosition=" + firstBytePos +
						" should be less then or equal to lastBytePosition=" + lastBytePos);
			}
		}

		@Override
		public long getRangeStart(long length) {
			return this.firstPos;
		}

		@Override
		public long getRangeEnd(long length) {
			if (this.lastPos != null && this.lastPos < length) {
				return this.lastPos;
			}
			else {
				return length - 1;
			}
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ByteRange)) {
				return false;
			}
			ByteRange otherRange = (ByteRange) other;
			return (this.firstPos == otherRange.firstPos &&
					ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 +
					ObjectUtils.nullSafeHashCode(this.lastPos));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.firstPos);
			builder.append('-');
			if (this.lastPos != null) {
				builder.append(this.lastPos);
			}
			return builder.toString();
		}
	}


	/**
	 * 表示 HTTP/1.1 后缀字节范围, 带有多个后缀字节.
	 */
	private static class SuffixByteRange extends HttpRange {

		private final long suffixLength;

		public SuffixByteRange(long suffixLength) {
			if (suffixLength < 0) {
				throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
			}
			this.suffixLength = suffixLength;
		}

		@Override
		public long getRangeStart(long length) {
			if (this.suffixLength < length) {
				return length - this.suffixLength;
			}
			else {
				return 0;
			}
		}

		@Override
		public long getRangeEnd(long length) {
			return length - 1;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SuffixByteRange)) {
				return false;
			}
			SuffixByteRange otherRange = (SuffixByteRange) other;
			return (this.suffixLength == otherRange.suffixLength);
		}

		@Override
		public int hashCode() {
			return ObjectUtils.hashCode(this.suffixLength);
		}

		@Override
		public String toString() {
			return "-" + this.suffixLength;
		}
	}

}
