package org.springframework.validation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * {@link MessageCodesResolver}接口的默认实现.
 *
 * <p>将按以下顺序为对象错误创建两个消息代码
 * (使用{@link Format#PREFIX_ERROR_CODE prefixed} {@link #setMessageCodeFormatter(MessageCodeFormatter) formatter}时):
 * <ul>
 * <li>1.: code + "." + object name
 * <li>2.: code
 * </ul>
 *
 * <p>将按以下顺序为字段规范创建四个消息代码:
 * <ul>
 * <li>1.: code + "." + object name + "." + field
 * <li>2.: code + "." + field
 * <li>3.: code + "." + field type
 * <li>4.: code
 * </ul>
 *
 * <p>例如, 在代码"typeMismatch"的情况下, 对象名称"user", 字段"age":
 * <ul>
 * <li>1. try "typeMismatch.user.age"
 * <li>2. try "typeMismatch.age"
 * <li>3. try "typeMismatch.int"
 * <li>4. try "typeMismatch"
 * </ul>
 *
 * <p>因此, 可以利用该解析算法来显示诸如"required" 和 "typeMismatch"之类的绑定错误的特定消息:
 * <ul>
 * <li>在对象 + 字段级别 ("age" 字段, 只包括"user"上的);
 * <li>在字段级别 (所有 "age" 字段, 不管其对象名称);
 * <li>或者在一般级别上 (所有对象上的所有字段).
 * </ul>
 *
 * <p>如果是数组, {@link List}或{@link java.util.Map}属性, 则会生成特定元素和整个集合的代码.
 * 假设对象"user"中数组"groups"的字段"name":
 * <ul>
 * <li>1. try "typeMismatch.user.groups[0].name"
 * <li>2. try "typeMismatch.user.groups.name"
 * <li>3. try "typeMismatch.groups[0].name"
 * <li>4. try "typeMismatch.groups.name"
 * <li>5. try "typeMismatch.name"
 * <li>6. try "typeMismatch.java.lang.String"
 * <li>7. try "typeMismatch"
 * </ul>
 *
 * <p>默认情况下, {@code errorCode}将放置在构造的消息字符串的开头.
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) messageCodeFormatter}属性可用于指定备用串联{@link MessageCodeFormatter format}.
 *
 * <p>为了将所有代码分组到资源包中的特定类别,
 * e.g. "validation.typeMismatch.name" 而不是默认的 "typeMismatch.name", 考虑指定要应用的{@link #setPrefix prefix}.
 */
@SuppressWarnings("serial")
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {

	/**
	 * 解析消息代码时此实现使用的分隔符.
	 */
	public static final String CODE_SEPARATOR = ".";

	private static final MessageCodeFormatter DEFAULT_FORMATTER = Format.PREFIX_ERROR_CODE;


	private String prefix = "";

	private MessageCodeFormatter formatter = DEFAULT_FORMATTER;


	/**
	 * 指定要应用于此解析器构建的任何代码的前缀.
	 * <p>默认无. 例如, 指定"validation." 获取错误代码, 如 "validation.typeMismatch.name".
	 */
	public void setPrefix(String prefix) {
		this.prefix = (prefix != null ? prefix : "");
	}

	/**
	 * 返回要应用于此解析器构建的任何代码的前缀.
	 * <p>如果没有前缀, 则返回空字符串.
	 */
	protected String getPrefix() {
		return this.prefix;
	}

	/**
	 * 指定此解析器生成的消息代码的格式.
	 * <p>默认是{@link Format#PREFIX_ERROR_CODE}.
	 */
	public void setMessageCodeFormatter(MessageCodeFormatter formatter) {
		this.formatter = (formatter != null ? formatter : DEFAULT_FORMATTER);
	}


	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName) {
		return resolveMessageCodes(errorCode, objectName, "", null);
	}

	/**
	 * 为给定的代码和字段构建代码列表:
	 * 特定于对象/字段的代码, 特定于字段的代码, 普通的错误代码.
	 * <p>对特定元素和整个集合解析数组、List和Map.
	 * <p>有关生成的代码的详细信息, 请参阅{@link DefaultMessageCodesResolver 类级别javadoc}.
	 * 
	 * @return 代码列表
	 */
	@Override
	public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType) {
		Set<String> codeList = new LinkedHashSet<String>();
		List<String> fieldList = new ArrayList<String>();
		buildFieldList(field, fieldList);
		addCodes(codeList, errorCode, objectName, fieldList);
		int dotIndex = field.lastIndexOf('.');
		if (dotIndex != -1) {
			buildFieldList(field.substring(dotIndex + 1), fieldList);
		}
		addCodes(codeList, errorCode, null, fieldList);
		if (fieldType != null) {
			addCode(codeList, errorCode, null, fieldType.getName());
		}
		addCode(codeList, errorCode, null, null);
		return StringUtils.toStringArray(codeList);
	}

	private void addCodes(Collection<String> codeList, String errorCode, String objectName, Iterable<String> fields) {
		for (String field : fields) {
			addCode(codeList, errorCode, objectName, field);
		}
	}

	private void addCode(Collection<String> codeList, String errorCode, String objectName, String field) {
		codeList.add(postProcessMessageCode(this.formatter.format(errorCode, objectName, field)));
	}

	/**
	 * 将提供的{@code field}的有Key的和没有Key的条目添加到提供的字段列表中.
	 */
	protected void buildFieldList(String field, List<String> fieldList) {
		fieldList.add(field);
		String plainField = field;
		int keyIndex = plainField.lastIndexOf('[');
		while (keyIndex != -1) {
			int endKeyIndex = plainField.indexOf(']', keyIndex);
			if (endKeyIndex != -1) {
				plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
				fieldList.add(plainField);
				keyIndex = plainField.lastIndexOf('[');
			}
			else {
				keyIndex = -1;
			}
		}
	}

	/**
	 * 对由此解析器构建的给定消息代码进行后处理.
	 * <p>默认实现应用指定的前缀.
	 * 
	 * @param code 由此解析器构建的消息代码
	 * 
	 * @return 要返回的最终消息代码
	 */
	protected String postProcessMessageCode(String code) {
		return getPrefix() + code;
	}


	/**
	 * 常见的消息代码格式.
	 */
	public enum Format implements MessageCodeFormatter {

		/**
		 * 在生成的消息代码的开头添加错误代码.
		 * e.g.: {@code errorCode + "." + object name + "." + field}
		 */
		PREFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, String objectName, String field) {
				return toDelimitedString(errorCode, objectName, field);
			}
		},

		/**
		 * 在生成的消息代码的末尾加上错误代码.
		 * e.g.: {@code object name + "." + field + "." + errorCode}
		 */
		POSTFIX_ERROR_CODE {
			@Override
			public String format(String errorCode, String objectName, String field) {
				return toDelimitedString(objectName, field, errorCode);
			}
		};

		/**
		 * 连接给定的元素, 用{@link DefaultMessageCodesResolver#CODE_SEPARATOR}分隔每个元素, 完全跳过零长度或null元素.
		 */
		public static String toDelimitedString(String... elements) {
			StringBuilder rtn = new StringBuilder();
			for (String element : elements) {
				if (StringUtils.hasLength(element)) {
					rtn.append(rtn.length() == 0 ? "" : CODE_SEPARATOR);
					rtn.append(element);
				}
			}
			return rtn.toString();
		}
	}
}
