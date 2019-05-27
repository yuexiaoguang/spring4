package org.springframework.core.type.filter;

import org.springframework.util.ClassUtils;

/**
 * 一个简单的过滤器, 它匹配可分配给给定类型的类.
 */
public class AssignableTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<?> targetType;


	/**
	 * @param targetType 要匹配的类型
	 */
	public AssignableTypeFilter(Class<?> targetType) {
		super(true, true);
		this.targetType = targetType;
	}


	@Override
	protected boolean matchClassName(String className) {
		return this.targetType.getName().equals(className);
	}

	@Override
	protected Boolean matchSuperClass(String superClassName) {
		return matchTargetType(superClassName);
	}

	@Override
	protected Boolean matchInterface(String interfaceName) {
		return matchTargetType(interfaceName);
	}

	protected Boolean matchTargetType(String typeName) {
		if (this.targetType.getName().equals(typeName)) {
			return true;
		}
		else if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				return this.targetType.isAssignableFrom(clazz);
			}
			catch (Throwable ex) {
				// 类不可定期加载 - 无法确定匹配方式.
			}
		}
		return null;
	}
}
