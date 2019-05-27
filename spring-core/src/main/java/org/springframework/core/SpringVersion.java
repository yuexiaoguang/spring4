package org.springframework.core;

/**
 * 暴露Spring版本的类.
 * 从jar文件中获取"Implementation-Version"清单属性.
 *
 * <p>请注意, 某些ClassLoader不公开包元数据, 因此该类可能无法在所有环境中确定Spring版本.
 * 请考虑使用基于反射的检查:
 * 例如, 检查是否存在要调用的特定Spring 2.0方法.
 */
public class SpringVersion {

	/**
	 * 返回当前Spring代码库的完整版本字符串; 如果无法确定, 则返回{@code null}.
	 */
	public static String getVersion() {
		Package pkg = SpringVersion.class.getPackage();
		return (pkg != null ? pkg.getImplementationVersion() : null);
	}

}
