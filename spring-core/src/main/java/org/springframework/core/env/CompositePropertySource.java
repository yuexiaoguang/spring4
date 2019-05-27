package org.springframework.core.env;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * 复合{@link PropertySource}实现, 它迭代一组{@link PropertySource}实例.
 * 在多个属性源共享相同名称的情况下是必要的, e.g. 当多个值提供给{@code @PropertySource}时.
 *
 * <p>从Spring 4.1.2开始, 此类扩展{@link EnumerablePropertySource}, 而不是普通的{@link PropertySource},
 * 根据所有包含的源(尽可能)累积的属性名称公开{@link #getPropertyNames()}.
 */
public class CompositePropertySource extends EnumerablePropertySource<Object> {

	private final Set<PropertySource<?>> propertySources = new LinkedHashSet<PropertySource<?>>();


	/**
	 * @param name 属性源的名称
	 */
	public CompositePropertySource(String name) {
		super(name);
	}


	@Override
	public Object getProperty(String name) {
		for (PropertySource<?> propertySource : this.propertySources) {
			Object candidate = propertySource.getProperty(name);
			if (candidate != null) {
				return candidate;
			}
		}
		return null;
	}

	@Override
	public boolean containsProperty(String name) {
		for (PropertySource<?> propertySource : this.propertySources) {
			if (propertySource.containsProperty(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] getPropertyNames() {
		Set<String> names = new LinkedHashSet<String>();
		for (PropertySource<?> propertySource : this.propertySources) {
			if (!(propertySource instanceof EnumerablePropertySource)) {
				throw new IllegalStateException(
						"Failed to enumerate property names due to non-enumerable property source: " + propertySource);
			}
			names.addAll(Arrays.asList(((EnumerablePropertySource<?>) propertySource).getPropertyNames()));
		}
		return StringUtils.toStringArray(names);
	}


	/**
	 * 将给定的{@link PropertySource}添加到链的末尾.
	 * 
	 * @param propertySource 要添加的PropertySource
	 */
	public void addPropertySource(PropertySource<?> propertySource) {
		this.propertySources.add(propertySource);
	}

	/**
	 * 将给定的{@link PropertySource}添加到链的开头.
	 * 
	 * @param propertySource 要添加的PropertySource
	 */
	public void addFirstPropertySource(PropertySource<?> propertySource) {
		List<PropertySource<?>> existing = new ArrayList<PropertySource<?>>(this.propertySources);
		this.propertySources.clear();
		this.propertySources.add(propertySource);
		this.propertySources.addAll(existing);
	}

	/**
	 * 返回此复合源包含的所有属性源.
	 */
	public Collection<PropertySource<?>> getPropertySources() {
		return this.propertySources;
	}


	@Override
	public String toString() {
		return String.format("%s [name='%s', propertySources=%s]",
				getClass().getSimpleName(), this.name, this.propertySources);
	}
}
