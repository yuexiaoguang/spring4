package org.springframework.core.io.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.core.io.Resource}数组的编辑器, 自动转换{@code String}位置模式
 * (e.g. {@code "file:C:/my*.txt"} 或{@code "classpath*:myfile.txt"})到{{@code Resource}数组属性.
 * 还可以将位置模式的集合或数组转换为合并的Resource数组.
 *
 * <p>路径可能包含{@code ${...}}占位符, 可以解析为{@link org.springframework.core.env.Environment}属性:
 * e.g. {@code ${user.dir}}.
 * 默认情况下会忽略无法解析的占位符.
 *
 * <p>委托给{@link ResourcePatternResolver}, 默认情况下使用{@link PathMatchingResourcePatternResolver}.
 */
public class ResourceArrayPropertyEditor extends PropertyEditorSupport {

	private static final Log logger = LogFactory.getLog(ResourceArrayPropertyEditor.class);

	private final ResourcePatternResolver resourcePatternResolver;

	private PropertyResolver propertyResolver;

	private final boolean ignoreUnresolvablePlaceholders;


	/**
	 * 使用默认的{@link PathMatchingResourcePatternResolver}和{@link StandardEnvironment}.
	 */
	public ResourceArrayPropertyEditor() {
		this(new PathMatchingResourcePatternResolver(), null, true);
	}

	/**
	 * @param resourcePatternResolver 要使用的ResourcePatternResolver
	 * @param propertyResolver 要使用的PropertyResolver (通常是{@link Environment})
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver, PropertyResolver propertyResolver) {
		this(resourcePatternResolver, propertyResolver, true);
	}

	/**
	 * @param resourcePatternResolver 要使用的ResourcePatternResolver
	 * @param propertyResolver 要使用的PropertyResolver (通常是{@link Environment})
	 * @param ignoreUnresolvablePlaceholders 如果找不到相应的系统属性, 是否忽略不可解析的占位符
	 */
	public ResourceArrayPropertyEditor(ResourcePatternResolver resourcePatternResolver,
			PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {

		Assert.notNull(resourcePatternResolver, "ResourcePatternResolver must not be null");
		this.resourcePatternResolver = resourcePatternResolver;
		this.propertyResolver = propertyResolver;
		this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
	}


	/**
	 * 将给定文本视为位置模式, 并将其转换为Resource数组.
	 */
	@Override
	public void setAsText(String text) {
		String pattern = resolvePath(text).trim();
		try {
			setValue(this.resourcePatternResolver.getResources(pattern));
		}
		catch (IOException ex) {
			throw new IllegalArgumentException(
					"Could not resolve resource location pattern [" + pattern + "]: " + ex.getMessage());
		}
	}

	/**
	 * 将给定值视为集合或数组, 并将其转换为Resource数组.
	 * 将String元素视为位置模式, 并按原样获取Resource元素.
	 */
	@Override
	public void setValue(Object value) throws IllegalArgumentException {
		if (value instanceof Collection || (value instanceof Object[] && !(value instanceof Resource[]))) {
			Collection<?> input = (value instanceof Collection ? (Collection<?>) value : Arrays.asList((Object[]) value));
			List<Resource> merged = new ArrayList<Resource>();
			for (Object element : input) {
				if (element instanceof String) {
					// 位置模式: 将其解析为Resource数组.
					// 可能指向单个资源或多个资源.
					String pattern = resolvePath((String) element).trim();
					try {
						Resource[] resources = this.resourcePatternResolver.getResources(pattern);
						for (Resource resource : resources) {
							if (!merged.contains(resource)) {
								merged.add(resource);
							}
						}
					}
					catch (IOException ex) {
						// ignore - 可能是未解析的占位符或不存在的基本目录
						if (logger.isDebugEnabled()) {
							logger.debug("Could not retrieve resources for pattern '" + pattern + "'", ex);
						}
					}
				}
				else if (element instanceof Resource) {
					// Resource对象: 将其添加到结果中.
					Resource resource = (Resource) element;
					if (!merged.contains(resource)) {
						merged.add(resource);
					}
				}
				else {
					throw new IllegalArgumentException("Cannot convert element [" + element + "] to [" +
							Resource.class.getName() + "]: only location String and Resource object supported");
				}
			}
			super.setValue(merged.toArray(new Resource[merged.size()]));
		}

		else {
			// 任意值: 可能是String或Resource数组.
			// 将为String调用setAsText; Resource数组将按原样使用.
			super.setValue(value);
		}
	}

	/**
	 * 解析给定路径, 必要时用相应的系统属性值替换占位符.
	 * 
	 * @param path 原始文件路径
	 * 
	 * @return 已解析的文件路径
	 */
	protected String resolvePath(String path) {
		if (this.propertyResolver == null) {
			this.propertyResolver = new StandardEnvironment();
		}
		return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) :
				this.propertyResolver.resolveRequiredPlaceholders(path));
	}
}
