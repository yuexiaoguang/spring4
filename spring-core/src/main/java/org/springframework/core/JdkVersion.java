package org.springframework.core;

/**
 * 内部帮助类, 用于查找Spring正在运行的Java/JVM版本, 以允许自动适应当前平台的功能.
 *
 * <p>请注意, 从Spring 4.0开始, Spring需要JVM 1.6或更高版本.
 *
 * @deprecated 从Spring 4.2.1开始, 支持通过反射直接检查所需的JDK API变体
 */
@Deprecated
public abstract class JdkVersion {

	/**
	 * 标识1.3.x JVM (JDK 1.3).
	 */
	public static final int JAVA_13 = 0;

	/**
	 * 标识1.4.x JVM (J2SE 1.4).
	 */
	public static final int JAVA_14 = 1;

	/**
	 * 标识1.5 JVM (Java 5).
	 */
	public static final int JAVA_15 = 2;

	/**
	 * 标识1.6 JVM (Java 6).
	 */
	public static final int JAVA_16 = 3;

	/**
	 * 标识1.7 JVM (Java 7).
	 */
	public static final int JAVA_17 = 4;

	/**
	 * 标识1.8 JVM (Java 8).
	 */
	public static final int JAVA_18 = 5;

	/**
	 * 标识1.9 JVM (Java 9).
	 */
	public static final int JAVA_19 = 6;


	private static final String javaVersion;

	private static final int majorJavaVersion;

	static {
		javaVersion = System.getProperty("java.version");
		// version String should look like "1.4.2_10"
		if (javaVersion.contains("1.9.")) {
			majorJavaVersion = JAVA_19;
		}
		else if (javaVersion.contains("1.8.")) {
			majorJavaVersion = JAVA_18;
		}
		else if (javaVersion.contains("1.7.")) {
			majorJavaVersion = JAVA_17;
		}
		else {
			// else leave 1.6 as default (it's either 1.6 or unknown)
			majorJavaVersion = JAVA_16;
		}
	}


	/**
	 * 返回完整的Java版本字符串, 由{@code System.getProperty("java.version")}返回.
	 * 
	 * @return 完整的Java版本字符串
	 */
	public static String getJavaVersion() {
		return javaVersion;
	}

	/**
	 * 获取主要版本代码. 意味着我们可以做{@code if (getMajorJavaVersion() >= JAVA_17)}之类的事情.
	 * 
	 * @return 与此类中的{@code JAVA_XX}代码相当的代码
	 */
	public static int getMajorJavaVersion() {
		return majorJavaVersion;
	}
}
