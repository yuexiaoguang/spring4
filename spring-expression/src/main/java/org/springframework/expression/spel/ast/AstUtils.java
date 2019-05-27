package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.PropertyAccessor;

/**
 * 用于Ast类的实用程序方法.
 */
public abstract class AstUtils {

	/**
	 * 确定应该用于尝试和访问指定目标类型的属性的属性解析器.
	 * 解析器被认为是在有序列表中, 但是在返回的列表中,
	 * 任何与输入目标类型完全匹配的 (与可能适用于任何类型的 '通用'解析器相对) 都放在列表的开头.
	 * 此外, 还有一些特定的解析器可以精确地命名相关的类, 还有一些解析器可以命名特定的类, 但它是我们所拥有的类的超类型.
	 * 这些放在特定解析器集的末尾, 并将在完全匹配的访问器之后, 但在通用访问器之前, 尝试.
	 * 
	 * @param targetType 正在尝试访问属性的类型
	 * 
	 * @return 应该尝试访问该属性的解析器列表
	 */
	public static List<PropertyAccessor> getPropertyAccessorsToTry(
			Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) {  // 通用解析器, 它可以用于任何类型
				generalAccessors.add(resolver);
			}
			else {
				if (targetType != null) {
					int pos = 0;
					for (Class<?> clazz : targets) {
						if (clazz == targetType) {  // 把精确的匹配放在前面, 先试试?
							specificAccessors.add(pos++, resolver);
						}
						else if (clazz.isAssignableFrom(targetType)) {  // 将超类型匹配放在最后
							// specificAccessor list
							generalAccessors.add(resolver);
						}
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new LinkedList<PropertyAccessor>();
		resolvers.addAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}

}
