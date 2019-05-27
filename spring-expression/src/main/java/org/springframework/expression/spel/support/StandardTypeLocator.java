package org.springframework.expression.spel.support;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.util.ClassUtils;

/**
 * {@link TypeLocator}的简单实现, 它使用上下文ClassLoader (或在其上设置的任何ClassLoader).
 * 它支持'众所周知的'包: 因此, 如果找不到类型, 它将尝试注册的导入来定位它.
 */
public class StandardTypeLocator implements TypeLocator {

	private final ClassLoader classLoader;

	private final List<String> knownPackagePrefixes = new LinkedList<String>();


	/**
	 * 为默认的ClassLoader创建StandardTypeLocator (通常是线程上下文ClassLoader).
	 */
	public StandardTypeLocator() {
		this(ClassUtils.getDefaultClassLoader());
	}

	/**
	 * @param classLoader 要委托给的ClassLoader
	 */
	public StandardTypeLocator(ClassLoader classLoader) {
		this.classLoader = classLoader;
		// 与编写常规Java代码类似, 默认情况下它只知道java.lang
		registerImport("java.lang");
	}


	/**
	 * 注册将在搜索非限定类型时使用的新导入前缀.
	 * 预期的格式类似于"java.lang".
	 * 
	 * @param prefix 要注册的前缀
	 */
	public void registerImport(String prefix) {
		this.knownPackagePrefixes.add(prefix);
	}

	/**
	 * 从此定位器的导入列表中删除该指定的前缀.
	 * 
	 * @param prefix 要删除的前缀
	 */
	public void removeImport(String prefix) {
		this.knownPackagePrefixes.remove(prefix);
	}

	/**
	 * 返回使用此StandardTypeLocator注册的所有导入前缀的列表.
	 * 
	 * @return 注册的所有导入前缀
	 */
	public List<String> getImportPrefixes() {
		return Collections.unmodifiableList(this.knownPackagePrefixes);
	}


	/**
	 * 查找(可能是非限定的)类型引用 - 首先使用类型名称, 然后尝试任何已注册的前缀, 如果找不到类型名称.
	 * 
	 * @param typeName 要查找的类型
	 * 
	 * @return 类型的Class对象
	 * @throws EvaluationException 如果找不到类型
	 */
	@Override
	public Class<?> findType(String typeName) throws EvaluationException {
		String nameToLookup = typeName;
		try {
			return ClassUtils.forName(nameToLookup, this.classLoader);
		}
		catch (ClassNotFoundException ey) {
			// 在放弃之前尝试任何已注册的前缀
		}
		for (String prefix : this.knownPackagePrefixes) {
			try {
				nameToLookup = prefix + '.' + typeName;
				return ClassUtils.forName(nameToLookup, this.classLoader);
			}
			catch (ClassNotFoundException ex) {
				// 可能是一个不同的前缀
			}
		}
		throw new SpelEvaluationException(SpelMessage.TYPE_NOT_FOUND, typeName);
	}

}
