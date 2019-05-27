package org.springframework.expression.spel.ast;

/**
 * 捕获原始类型及其对应的类对象, 以及一个表示所有引用 (非原始)类型的特殊条目.
 */
public enum TypeCode {

	OBJECT(Object.class),

	BOOLEAN(Boolean.TYPE),

	BYTE(Byte.TYPE),

	CHAR(Character.TYPE),

	DOUBLE(Double.TYPE),

	FLOAT(Float.TYPE),

	INT(Integer.TYPE),

	LONG(Long.TYPE),

	SHORT(Short.TYPE);


	private Class<?> type;


	TypeCode(Class<?> type) {
		this.type = type;
	}


	public Class<?> getType() {
		return this.type;
	}


	public static TypeCode forName(String name) {
		String searchingFor = name.toUpperCase();
		TypeCode[] tcs = values();
		for (int i = 1; i < tcs.length; i++) {
			if (tcs[i].name().equals(searchingFor)) {
				return tcs[i];
			}
		}
		return OBJECT;
	}

	public static TypeCode forClass(Class<?> clazz) {
		TypeCode[] allValues = TypeCode.values();
		for (TypeCode typeCode : allValues) {
			if (clazz == typeCode.getType()) {
				return typeCode;
			}
		}
		return OBJECT;
	}

}
