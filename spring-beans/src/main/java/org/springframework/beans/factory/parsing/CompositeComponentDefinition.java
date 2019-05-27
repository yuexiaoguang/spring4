package org.springframework.beans.factory.parsing;

import java.util.LinkedList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * {@link ComponentDefinition}实现, 包含一个或多个嵌套的{@link ComponentDefinition}实例,
 * 将它们聚合到一组命名的组件中.
 */
public class CompositeComponentDefinition extends AbstractComponentDefinition {

	private final String name;

	private final Object source;

	private final List<ComponentDefinition> nestedComponents = new LinkedList<ComponentDefinition>();


	/**
	 * @param name 复合组件的名称
	 * @param source 定义复合组件根的source元素
	 */
	public CompositeComponentDefinition(String name, Object source) {
		Assert.notNull(name, "Name must not be null");
		this.name = name;
		this.source = source;
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Object getSource() {
		return this.source;
	}


	/**
	 * 将给定组件添加为此组合组件的嵌套元素.
	 * 
	 * @param component 要添加的嵌套组件
	 */
	public void addNestedComponent(ComponentDefinition component) {
		Assert.notNull(component, "ComponentDefinition must not be null");
		this.nestedComponents.add(component);
	}

	/**
	 * 返回此复合组件包含的嵌套组件.
	 * 
	 * @return 嵌套组件数组, 或空数组
	 */
	public ComponentDefinition[] getNestedComponents() {
		return this.nestedComponents.toArray(new ComponentDefinition[this.nestedComponents.size()]);
	}

}
