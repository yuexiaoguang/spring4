package org.springframework.http;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.comparator.CompoundComparator;

/**
 * {@link MimeType}的子类, 它增加了对HTTP规范中定义的质量参数的支持.
 */
public class MediaType extends MimeType implements Serializable {

	private static final long serialVersionUID = 2069937152339670231L;

	/**
	 * 包含所有媒体范围的公共常量媒体类型 (i.e. "&#42;/&#42;").
	 */
	public static final MediaType ALL;

	/**
	 * 等同于{@link MediaType#ALL}.
	 */
	public static final String ALL_VALUE = "*/*";

	/**
	 * 等同于{@code application/atom+xml}.
	 */
	public final static MediaType APPLICATION_ATOM_XML;

	/**
	 * 等同于{@link MediaType#APPLICATION_ATOM_XML}.
	 */
	public final static String APPLICATION_ATOM_XML_VALUE = "application/atom+xml";

	/**
	 * 等同于{@code application/x-www-form-urlencoded}.
	 */
	public final static MediaType APPLICATION_FORM_URLENCODED;

	/**
	 * 等同于{@link MediaType#APPLICATION_FORM_URLENCODED}.
	 */
	public final static String APPLICATION_FORM_URLENCODED_VALUE = "application/x-www-form-urlencoded";

	/**
	 * 等同于{@code application/json}.
	 */
	public final static MediaType APPLICATION_JSON;

	/**
	 * 等同于{@link MediaType#APPLICATION_JSON}.
	 */
	public final static String APPLICATION_JSON_VALUE = "application/json";

	/**
	 * 等同于{@code application/json;charset=UTF-8}.
	 */
	public final static MediaType APPLICATION_JSON_UTF8;

	/**
	 * 等同于{@link MediaType#APPLICATION_JSON_UTF8}.
	 */
	public final static String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";

	/**
	 * 等同于{@code application/octet-stream}.
	 */
	public final static MediaType APPLICATION_OCTET_STREAM;

	/**
	 * 等同于{@link MediaType#APPLICATION_OCTET_STREAM}.
	 */
	public final static String APPLICATION_OCTET_STREAM_VALUE = "application/octet-stream";

	/**
	 * 等同于{@code application/pdf}.
	 */
	public final static MediaType APPLICATION_PDF;

	/**
	 * 等同于{@link MediaType#APPLICATION_PDF}.
	 */
	public final static String APPLICATION_PDF_VALUE = "application/pdf";

	/**
	 * 等同于{@code application/rss+xml}.
	 */
	public final static MediaType APPLICATION_RSS_XML;

	/**
	 * 等同于{@link MediaType#APPLICATION_RSS_XML}.
	 */
	public final static String APPLICATION_RSS_XML_VALUE = "application/rss+xml";

	/**
	 * 等同于{@code application/xhtml+xml}.
	 */
	public final static MediaType APPLICATION_XHTML_XML;

	/**
	 * 等同于{@link MediaType#APPLICATION_XHTML_XML}.
	 */
	public final static String APPLICATION_XHTML_XML_VALUE = "application/xhtml+xml";

	/**
	 * 等同于{@code application/xml}.
	 */
	public final static MediaType APPLICATION_XML;

	/**
	 * 等同于{@link MediaType#APPLICATION_XML}.
	 */
	public final static String APPLICATION_XML_VALUE = "application/xml";

	/**
	 * 等同于{@code image/gif}.
	 */
	public final static MediaType IMAGE_GIF;

	/**
	 * 等同于{@link MediaType#IMAGE_GIF}.
	 */
	public final static String IMAGE_GIF_VALUE = "image/gif";

	/**
	 * 等同于{@code image/jpeg}.
	 */
	public final static MediaType IMAGE_JPEG;

	/**
	 * 等同于{@link MediaType#IMAGE_JPEG}.
	 */
	public final static String IMAGE_JPEG_VALUE = "image/jpeg";

	/**
	 * 等同于{@code image/png}.
	 */
	public final static MediaType IMAGE_PNG;

	/**
	 * 等同于{@link MediaType#IMAGE_PNG}.
	 */
	public final static String IMAGE_PNG_VALUE = "image/png";

	/**
	 * 等同于{@code multipart/form-data}.
	 */
	public final static MediaType MULTIPART_FORM_DATA;

	/**
	 * 等同于{@link MediaType#MULTIPART_FORM_DATA}.
	 */
	public final static String MULTIPART_FORM_DATA_VALUE = "multipart/form-data";

	/**
	 * 等同于{@code text/event-stream}.
	 */
	public final static MediaType TEXT_EVENT_STREAM;

	/**
	 * 等同于{@link MediaType#TEXT_EVENT_STREAM}.
	 */
	public final static String TEXT_EVENT_STREAM_VALUE = "text/event-stream";

	/**
	 * 等同于{@code text/html}.
	 */
	public final static MediaType TEXT_HTML;

	/**
	 * 等同于{@link MediaType#TEXT_HTML}.
	 */
	public final static String TEXT_HTML_VALUE = "text/html";

	/**
	 * 等同于{@code text/markdown}.
	 */
	public final static MediaType TEXT_MARKDOWN;

	/**
	 * 等同于{@link MediaType#TEXT_MARKDOWN}.
	 */
	public final static String TEXT_MARKDOWN_VALUE = "text/markdown";

	/**
	 * 等同于{@code text/plain}.
	 */
	public final static MediaType TEXT_PLAIN;

	/**
	 * 等同于{@link MediaType#TEXT_PLAIN}.
	 */
	public final static String TEXT_PLAIN_VALUE = "text/plain";

	/**
	 * 等同于{@code text/xml}.
	 */
	public final static MediaType TEXT_XML;

	/**
	 * 等同于{@link MediaType#TEXT_XML}.
	 */
	public final static String TEXT_XML_VALUE = "text/xml";

	private static final String PARAM_QUALITY_FACTOR = "q";


	static {
		ALL = valueOf(ALL_VALUE);
		APPLICATION_ATOM_XML = valueOf(APPLICATION_ATOM_XML_VALUE);
		APPLICATION_FORM_URLENCODED = valueOf(APPLICATION_FORM_URLENCODED_VALUE);
		APPLICATION_JSON = valueOf(APPLICATION_JSON_VALUE);
		APPLICATION_JSON_UTF8 = valueOf(APPLICATION_JSON_UTF8_VALUE);
		APPLICATION_OCTET_STREAM = valueOf(APPLICATION_OCTET_STREAM_VALUE);
		APPLICATION_PDF = valueOf(APPLICATION_PDF_VALUE);
		APPLICATION_RSS_XML = valueOf(APPLICATION_RSS_XML_VALUE);
		APPLICATION_XHTML_XML = valueOf(APPLICATION_XHTML_XML_VALUE);
		APPLICATION_XML = valueOf(APPLICATION_XML_VALUE);
		IMAGE_GIF = valueOf(IMAGE_GIF_VALUE);
		IMAGE_JPEG = valueOf(IMAGE_JPEG_VALUE);
		IMAGE_PNG = valueOf(IMAGE_PNG_VALUE);
		MULTIPART_FORM_DATA = valueOf(MULTIPART_FORM_DATA_VALUE);
		TEXT_EVENT_STREAM = valueOf(TEXT_EVENT_STREAM_VALUE);
		TEXT_HTML = valueOf(TEXT_HTML_VALUE);
		TEXT_MARKDOWN = valueOf(TEXT_MARKDOWN_VALUE);
		TEXT_PLAIN = valueOf(TEXT_PLAIN_VALUE);
		TEXT_XML = valueOf(TEXT_XML_VALUE);
	}


	/**
	 * <p>{@linkplain #getSubtype() 子类型}设置为"&#42;", 参数为空.
	 * 
	 * @param type 主要类型
	 * 
	 * @throws IllegalArgumentException 如果任何参数包含非法字符
	 */
	public MediaType(String type) {
		super(type);
	}

	/**
	 * <p>参数为空.
	 * 
	 * @param type 主要类型
	 * @param subtype 子类型
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(String type, String subtype) {
		super(type, subtype, Collections.<String, String>emptyMap());
	}

	/**
	 * @param type 主要类型
	 * @param subtype 子类型
	 * @param charset 字符集
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(String type, String subtype, Charset charset) {
		super(type, subtype, charset);
	}

	/**
	 * @param type 主要类型
	 * @param subtype 子类型
	 * @param qualityValue 质量值
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(String type, String subtype, double qualityValue) {
		this(type, subtype, Collections.singletonMap(PARAM_QUALITY_FACTOR, Double.toString(qualityValue)));
	}

	/**
	 * 复制构造函数, 复制给定{@code MediaType}的类型, 子类型和参数, 并允许设置指定的字符集.
	 * 
	 * @param other 其他媒体类型
	 * @param charset 字符集
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(MediaType other, Charset charset) {
		super(other, charset);
	}

	/**
	 * 复制构造函数, 复制给定{@code MediaType}的类型和子类型, 并允许不同的参数.
	 * 
	 * @param other 其他媒体类型
	 * @param parameters 参数, 可以是{@code null}
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(MediaType other, Map<String, String> parameters) {
		super(other.getType(), other.getSubtype(), parameters);
	}

	/**
	 * @param type 主要类型
	 * @param subtype 子类型
	 * @param parameters 参数, 可以是{@code null}
	 * 
	 * @throws IllegalArgumentException 如果参数包含非法字符
	 */
	public MediaType(String type, String subtype, Map<String, String> parameters) {
		super(type, subtype, parameters);
	}


	@Override
	protected void checkParameters(String attribute, String value) {
		super.checkParameters(attribute, value);
		if (PARAM_QUALITY_FACTOR.equals(attribute)) {
			value = unquote(value);
			double d = Double.parseDouble(value);
			Assert.isTrue(d >= 0D && d <= 1D,
					"Invalid quality value \"" + value + "\": should be between 0.0 and 1.0");
		}
	}

	/**
	 * 返回质量因子, 如{@code q}参数所示.
	 * 默认{@code 1.0}.
	 * 
	 * @return 质量因子
	 */
	public double getQualityValue() {
		String qualityFactor = getParameter(PARAM_QUALITY_FACTOR);
		return (qualityFactor != null ? Double.parseDouble(unquote(qualityFactor)) : 1D);
	}

	/**
	 * 指示此{@code MediaType}是否包含给定的媒体类型.
	 * <p>例如, {@code text/*}包括 {@code text/plain}和 {@code text/html},
	 * 以及{@code application/*+xml}包括 {@code application/soap+xml}, 等.
	 * 这种方法<b>不</b>对称.
	 * <p>只需调用{@link #includes(MimeType)}, 但使用{@code MediaType}参数声明用于二进制向后兼容性.
	 * 
	 * @param other 要与之比较的参考媒体类型
	 * 
	 * @return {@code true} 如果此媒体类型包含给定的媒体类型; 否则{@code false}
	 */
	public boolean includes(MediaType other) {
		return super.includes(other);
	}

	/**
	 * 指示此{@code MediaType}是否与给定的媒体类型兼容.
	 * <p>例如, {@code text/*}与{@code text/plain}, {@code text/html}兼容, 反之亦然.
	 * 实际上, 此方法类似于{@link #includes}, 除了它<b>是</b>对称的.
	 * <p>只需调用{@link #isCompatibleWith(MimeType)}, 但使用{@code MediaType}参数声明用于二进制向后兼容性.
	 * 
	 * @param other 要与之比较的参考媒体类型
	 * 
	 * @return {@code true} 如果此媒体类型与给定的媒体类型兼容; 否则{@code false}
	 */
	public boolean isCompatibleWith(MediaType other) {
		return super.isCompatibleWith(other);
	}

	/**
	 * 使用给定{@code MediaType}的质量值返回此实例的副本.
	 * 
	 * @return 如果给定的MediaType没有质量值, 则为相同的实例, 否则为新的
	 */
	public MediaType copyQualityValue(MediaType mediaType) {
		if (!mediaType.getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<String, String>(getParameters());
		params.put(PARAM_QUALITY_FACTOR, mediaType.getParameters().get(PARAM_QUALITY_FACTOR));
		return new MediaType(this, params);
	}

	/**
	 * 返回此实例的副本, 并删除其质量值.
	 * 
	 * @return 如果媒体类型不包含质量值, 则为相同的实例, 否则为新的
	 */
	public MediaType removeQualityValue() {
		if (!getParameters().containsKey(PARAM_QUALITY_FACTOR)) {
			return this;
		}
		Map<String, String> params = new LinkedHashMap<String, String>(getParameters());
		params.remove(PARAM_QUALITY_FACTOR);
		return new MediaType(this, params);
	}


	/**
	 * 将给定的String值解析为{@code MediaType}对象, 此方法名称遵循'valueOf'命名约定
	 * (由{@link org.springframework.core.convert.ConversionService}支持).
	 * 
	 * @param value 要解析的字符串
	 * 
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static MediaType valueOf(String value) {
		return parseMediaType(value);
	}

	/**
	 * 将给定的String解析为单个{@code MediaType}.
	 * 
	 * @param mediaType 要解析的字符串
	 * 
	 * @return 媒体类型
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static MediaType parseMediaType(String mediaType) {
		MimeType type;
		try {
			type = MimeTypeUtils.parseMimeType(mediaType);
		}
		catch (InvalidMimeTypeException ex) {
			throw new InvalidMediaTypeException(ex);
		}
		try {
			return new MediaType(type.getType(), type.getSubtype(), type.getParameters());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidMediaTypeException(mediaType, ex.getMessage());
		}
	}

	/**
	 * 将给定的逗号分隔的字符串解析为{@code MediaType}对象列表.
	 * <p>此方法可用于解析 Accept 或 Content-Type header.
	 * 
	 * @param mediaTypes 要解析的字符串
	 * 
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static List<MediaType> parseMediaTypes(String mediaTypes) {
		if (!StringUtils.hasLength(mediaTypes)) {
			return Collections.emptyList();
		}
		String[] tokens = StringUtils.tokenizeToStringArray(mediaTypes, ",");
		List<MediaType> result = new ArrayList<MediaType>(tokens.length);
		for (String token : tokens) {
			result.add(parseMediaType(token));
		}
		return result;
	}

	/**
	 * 将给定的(可能)逗号分隔的字符串列表解析为{@code MediaType}对象列表.
	 * <p>此方法可用于解析Accept 或 Content-Type header.
	 * 
	 * @param mediaTypes 要解析的字符串
	 * 
	 * @return 媒体类型列表
	 * @throws InvalidMediaTypeException 如果无法解析媒体类型值
	 */
	public static List<MediaType> parseMediaTypes(List<String> mediaTypes) {
		if (CollectionUtils.isEmpty(mediaTypes)) {
			return Collections.<MediaType>emptyList();
		}
		else if (mediaTypes.size() == 1) {
			return parseMediaTypes(mediaTypes.get(0));
		}
		else {
			List<MediaType> result = new ArrayList<MediaType>(8);
			for (String mediaType : mediaTypes) {
				result.addAll(parseMediaTypes(mediaType));
			}
			return result;
		}
	}

	/**
	 * 返回给定{@code MediaType}对象列表的字符串表示形式.
	 * <p>此方法可用于{@code Accept} 或 {@code Content-Type} header.
	 * 
	 * @param mediaTypes 要为其创建字符串表示形式的媒体类型
	 * 
	 * @return 字符串表示
	 */
	public static String toString(Collection<MediaType> mediaTypes) {
		return MimeTypeUtils.toString(mediaTypes);
	}

	/**
	 * 按特定方式对给定的{@code MediaType}对象列表进行排序.
	 * <p>给定两种媒体类型:
	 * <ol>
	 * <li>如果任一媒体类型具有{@linkplain #isWildcardType() 通配符类型}, 则没有通配符的媒体类型排在前面.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getType() 类型}, 那么它们被视为相等并保持其当前顺序.</li>
	 * <li>如果任一媒体类型具有{@linkplain #isWildcardSubtype() 通配符子类型}, 则不带通配符的媒体类型排在前面.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getSubtype() 子类型}, 那么它们被视为相等并保持其当前顺序.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getQualityValue() 质量值}, 则具有最高质量值的媒体类型排在前面.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getParameter(String) 参数}, 则具有最多参数的媒体类型排在前面.</li>
	 * </ol>
	 * <p>示例:
	 * <blockquote>audio/basic &lt; audio/* &lt; *&#047;*</blockquote>
	 * <blockquote>audio/* &lt; audio/*;q=0.7; audio/*;q=0.3</blockquote>
	 * <blockquote>audio/basic;level=1 &lt; audio/basic</blockquote>
	 * <blockquote>audio/basic == text/html</blockquote>
	 * <blockquote>audio/basic == audio/wave</blockquote>
	 * 
	 * @param mediaTypes 要排序的媒体类型列表
	 */
	public static void sortBySpecificity(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, SPECIFICITY_COMPARATOR);
		}
	}

	/**
	 * 按质量值对给定的{@code MediaType}对象列表进行排序.
	 * <p>给定两种媒体类型:
	 * <ol>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getQualityValue() 质量值}, 则具有最高质量值的媒体类型排在前面.</li>
	 * <li>如果任一媒体类型具有{@linkplain #isWildcardType() 通配符类型}, 则没有通配符的媒体类型排在前面.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getType() 类型}, 那么它们被视为相等并保持其当前顺序.</li>
	 * <li>如果任一媒体类型具有{@linkplain #isWildcardSubtype() 通配符子类型}, 则不带通配符的媒体类型排在前面.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getSubtype() 子类型}, 那么它们被视为相等并保持其当前顺序.</li>
	 * <li>如果两种媒体类型具有不同的{@linkplain #getParameter(String) 参数}, 则具有最多参数的媒体类型排在前面.</li>
	 * </ol>
	 * 
	 * @param mediaTypes 要排序的媒体类型列表
	 */
	public static void sortByQualityValue(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, QUALITY_VALUE_COMPARATOR);
		}
	}

	/**
	 * 按特定方式对给定的{@code MediaType}对象列表进行排序作为主要标准, 质量值排序为次要标准.
	 */
	public static void sortBySpecificityAndQuality(List<MediaType> mediaTypes) {
		Assert.notNull(mediaTypes, "'mediaTypes' must not be null");
		if (mediaTypes.size() > 1) {
			Collections.sort(mediaTypes, new CompoundComparator<MediaType>(
					MediaType.SPECIFICITY_COMPARATOR, MediaType.QUALITY_VALUE_COMPARATOR));
		}
	}


	/**
	 * {@link #sortByQualityValue(List)}使用的比较器.
	 */
	public static final Comparator<MediaType> QUALITY_VALUE_COMPARATOR = new Comparator<MediaType>() {

		@Override
		public int compare(MediaType mediaType1, MediaType mediaType2) {
			double quality1 = mediaType1.getQualityValue();
			double quality2 = mediaType2.getQualityValue();
			int qualityComparison = Double.compare(quality2, quality1);
			if (qualityComparison != 0) {
				return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
			}
			else if (mediaType1.isWildcardType() && !mediaType2.isWildcardType()) { // */* < audio/*
				return 1;
			}
			else if (mediaType2.isWildcardType() && !mediaType1.isWildcardType()) { // audio/* > */*
				return -1;
			}
			else if (!mediaType1.getType().equals(mediaType2.getType())) { // audio/basic == text/html
				return 0;
			}
			else { // mediaType1.getType().equals(mediaType2.getType())
				if (mediaType1.isWildcardSubtype() && !mediaType2.isWildcardSubtype()) { // audio/* < audio/basic
					return 1;
				}
				else if (mediaType2.isWildcardSubtype() && !mediaType1.isWildcardSubtype()) { // audio/basic > audio/*
					return -1;
				}
				else if (!mediaType1.getSubtype().equals(mediaType2.getSubtype())) { // audio/basic == audio/wave
					return 0;
				}
				else {
					int paramsSize1 = mediaType1.getParameters().size();
					int paramsSize2 = mediaType2.getParameters().size();
					// audio/basic;level=1 < audio/basic
					return (paramsSize2 < paramsSize1 ? -1 : (paramsSize2 == paramsSize1 ? 0 : 1));
				}
			}
		}
	};


	/**
	 * {@link #sortBySpecificity(List)}使用的比较器.
	 */
	public static final Comparator<MediaType> SPECIFICITY_COMPARATOR = new SpecificityComparator<MediaType>() {

		@Override
		protected int compareParameters(MediaType mediaType1, MediaType mediaType2) {
			double quality1 = mediaType1.getQualityValue();
			double quality2 = mediaType2.getQualityValue();
			int qualityComparison = Double.compare(quality2, quality1);
			if (qualityComparison != 0) {
				return qualityComparison;  // audio/*;q=0.7 < audio/*;q=0.3
			}
			return super.compareParameters(mediaType1, mediaType2);
		}
	};

}
