package org.springframework.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 表示由HTML 4.0标准定义的一组字符实体引用.
 *
 * <p>有关HTML 4.0字符集的完整说明, 请访问 http://www.w3.org/TR/html4/charset.html.
 */
class HtmlCharacterEntityReferences {

	private static final String PROPERTIES_FILE = "HtmlCharacterEntityReferences.properties";

	static final char REFERENCE_START = '&';

	static final String DECIMAL_REFERENCE_START = "&#";

	static final String HEX_REFERENCE_START = "&#x";

	static final char REFERENCE_END = ';';

	static final char CHAR_NULL = (char) -1;


	private final String[] characterToEntityReferenceMap = new String[3000];

	private final Map<String, Character> entityReferenceToCharacterMap = new HashMap<String, Character>(512);


	/**
	 * 返回反映HTML 4.0字符集的新的字符实体引用的集合.
	 */
	public HtmlCharacterEntityReferences() {
		Properties entityReferences = new Properties();

		// 加载引用定义文件
		InputStream is = HtmlCharacterEntityReferences.class.getResourceAsStream(PROPERTIES_FILE);
		if (is == null) {
			throw new IllegalStateException(
					"Cannot find reference definition file [HtmlCharacterEntityReferences.properties] as class path resource");
		}
		try {
			try {
				entityReferences.load(is);
			}
			finally {
				is.close();
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(
					"Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " +  ex.getMessage());
		}

		// 解析引用定义属性
		Enumeration<?> keys = entityReferences.propertyNames();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			int referredChar = Integer.parseInt(key);
			if (!(referredChar < 1000 || (referredChar >= 8000 && referredChar < 10000))) {
				throw new IllegalArgumentException("Invalid reference to special HTML entity: " + referredChar);
			}
			int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
			String reference = entityReferences.getProperty(key);
			this.characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
			this.entityReferenceToCharacterMap.put(reference, (char) referredChar);
		}
	}


	/**
	 * 返回支持的实体引用数量.
	 */
	public int getSupportedReferenceCount() {
		return this.entityReferenceToCharacterMap.size();
	}

	/**
	 * 如果给定字符映射到受支持的实体引用, 则返回true.
	 */
	public boolean isMappedToReference(char character) {
		return isMappedToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 如果给定字符映射到受支持的实体引用, 则返回true.
	 */
	public boolean isMappedToReference(char character, String encoding) {
		return (convertToReference(character, encoding) != null);
	}

	/**
	 * 返回映射到给定字符的引用, 或{@code null}.
	 */
	public String convertToReference(char character) {
	   return convertToReference(character, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 返回映射到给定字符的引用, 或{@code null}.
	 */
	public String convertToReference(char character, String encoding) {
		if (encoding.startsWith("UTF-")){
			switch (character){
				case '<':
					return "&lt;";
				case '>':
					return "&gt;";
				case '"':
					return "&quot;";
				case '&':
					return "&amp;";
				case '\'':
					return "&#39;";
			}
		}
		else if (character < 1000 || (character >= 8000 && character < 10000)) {
			int index = (character < 1000 ? character : character - 7000);
			String entityReference = this.characterToEntityReferenceMap[index];
			if (entityReference != null) {
				return entityReference;
			}
		}
		return null;
	}

	/**
	 * 返回映射到给定entityReference或-1的char.
	 */
	public char convertToCharacter(String entityReference) {
		Character referredCharacter = this.entityReferenceToCharacterMap.get(entityReference);
		if (referredCharacter != null) {
			return referredCharacter;
		}
		return CHAR_NULL;
	}

}
