package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.util.StringUtils;

/**
 * 检测XML流是否使用基于DTD或XSD的验证.
 */
public class XmlValidationModeDetector {

	/**
	 * 表示应禁用验证.
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * 表示验证模式应该是自动猜测的, 因为找不到清晰的指示 (可能在某些特殊字符上被阻塞等).
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * 表示应该使用DTD验证 (找到了"DOCTYPE"声明).
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * 表示应该使用XSD验证(找不到"DOCTYPE"声明).
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * XML文档中的标记, 声明DTD用于验证, 因此正在使用DTD验证.
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * 表示XML注释开始的标记.
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * 表示XML注释结束的标记.
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * 指示当前解析位置是否在XML注释内.
	 */
	private boolean inComment;


	/**
	 * 在提供的{@link InputStream}中检测XML文档的验证模式.
	 * 请注意, 在返回之前, 此方法将关闭提供的{@link InputStream}.
	 * 
	 * @param inputStream 要解析的InputStream
	 * 
	 * @throws IOException 发生I/O失败
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		// 查看文件以查找DOCTYPE.
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			boolean isDtdValidated = false;
			String content;
			while ((content = reader.readLine()) != null) {
				content = consumeCommentTokens(content);
				if (this.inComment || !StringUtils.hasText(content)) {
					continue;
				}
				if (hasDoctype(content)) {
					isDtdValidated = true;
					break;
				}
				if (hasOpeningTag(content)) {
					// End of meaningful data...
					break;
				}
			}
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		}
		catch (CharConversionException ex) {
			// 在一些字符编码上窒息...
			// 将决定权交给调用者.
			return VALIDATION_AUTO;
		}
		finally {
			reader.close();
		}
	}


	/**
	 * 内容是否包含DTD DOCTYPE声明?
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * 提供的内容是否包含XML开始标记.
	 * 如果解析状态当前在XML注释中, 则此方法始终返回false.
	 * 在将剩余的传递给此方法之前, 预计所有注释令牌将消耗所提供的内容.
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 && (content.length() > openTagIndex + 1) &&
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * 消费给定String中的所有前导注释数据, 并返回剩余内容, 这些内容可能为空, 因为提供的内容可能是所有注释数据.
	 * 出于我们的目的, 重要的是剥离一行上的前导注释内容, 因为第一条非注释内容将是DOCTYPE声明或文档的根元素.
	 */
	private String consumeCommentTokens(String line) {
		if (!line.contains(START_COMMENT) && !line.contains(END_COMMENT)) {
			return line;
		}
		while ((line = consume(line)) != null) {
			if (!this.inComment && !line.trim().startsWith(START_COMMENT)) {
				return line;
			}
		}
		return line;
	}

	/**
	 * 消费下一个注释标记, 更新"inComment"标志并返回剩余的内容.
	 */
	private String consume(String line) {
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * 尝试消费{@link #START_COMMENT} token.
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * 尝试针对提供的内容消费提供的token, 并将注释解析状态更新为提供的值.
	 * 返回标记之后的内容的索引; 如果未找到标记, 则返回-1.
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > - 1) {
			this.inComment = inCommentIfPresent;
		}
		return (index == -1 ? index : index + token.length());
	}
}
