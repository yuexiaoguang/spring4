package org.springframework.beans.factory.parsing;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;

/**
 * 描述一些配置上下文中显示的一组{@link BeanDefinition BeanDefinitions}和{@link BeanReference BeanReferences}的逻辑视图的接口.
 *
 * <p>随着{@link org.springframework.beans.factory.xml.NamespaceHandler 可插入自定义XML标签}的引入,
 * 现在, 单个逻辑配置实体(在本例中为XML标记)可以创建多个{@link BeanDefinition BeanDefinitions}
 * 和{@link BeanReference RuntimeBeanReferences}, 为了给最终用户提供更简洁的配置和更大的便利.
 * 因此, 不能再假设每个配置实体(e.g. XML标签)映射到一个{@link BeanDefinition}.
 * 对于希望提供可视化或支持配置Spring应用程序的工具供应商和其他用户, 有一些机制将{@link BeanDefinition BeanDefinitions}
 * 与{@link org.springframework.beans.factory.BeanFactory}中的{@link BeanDefinition BeanDefinitions}绑定回配置数据, 这对最终用户具有实际意义.
 * 因此, {@link org.springframework.beans.factory.xml.NamespaceHandler}实现能够以{@code ComponentDefinition}的形式为正在配置的每个逻辑实体发布事件.
 * 然后，第三方可以 {@link ReaderEventListener 订阅这些事件}, 允许以用户为中心的bean元数据视图.
 *
 * <p>每个{@code ComponentDefinition}都有一个{@link #getSource源对象}, 它是特定于配置的.
 * 对于基于XML的配置, 这通常是{@link org.w3c.dom.Node}, 其中包含用户提供的配置信息.
 * 除此之外, {@code ComponentDefinition}中包含的每个{@link BeanDefinition}都有自己的 {@link BeanDefinition#getSource() 源对象},
 * 它可能指向一组不同的, 更具体的配置数据.
 * 除此之外, 诸如{@link org.springframework.beans.PropertyValue PropertyValues}之类的bean元数据也可能具有源对象, 从而提供更高级别的详细信息.
 * 源对象的提取可以通过{@link SourceExtractor}处理, 可以根据需要进行自定义.
 *
 * <p>虽然通过{@link #getBeanReferences}提供了对重要{@link BeanReference BeanReferences}的直接访问,
 * 工具可能希望检查所有的{@link BeanDefinition BeanDefinitions}以收集{@link BeanReference BeanReferences}的完整集合.
 * 需要实现来提供所有, 验证整个逻辑实体的配置所需的, 以及配置完整的用户可视化所需的, {@link BeanReference BeanReferences}.
 * 预计某些{@link BeanReference BeanReferences}对验证或配置的用户视图不重要, 因此这些可能会被省略.
 * 工具可能希望显示源自提供的{@link BeanDefinition BeanDefinitions}的其他{@link BeanReference BeanReferences}, 但这不被视为典型案例.
 *
 * <p>通过检查{@link BeanDefinition#getRole 角色标识符}, 工具可以确定包含的{@link BeanDefinition BeanDefinitions}的重要性.
 * 该角色本质上是该工具的一个暗示, 即配置提供者认为{@link BeanDefinition}对最终用户的重要性.
 * 对于给定的{@code ComponentDefinition}, 预计工具不会显示所有的{@link BeanDefinition BeanDefinitions}, 而是根据角色进行筛选.
 * 工具可以选择使此过滤用户配置.
 * 应特别注意 {@link BeanDefinition#ROLE_INFRASTRUCTURE INFRASTRUCTURE 角色标识符}.
 * 使用此角色分类的{@link BeanDefinition BeanDefinitions} 对最终用户完全不重要, 仅出于内部实现原因而需要.
 */
public interface ComponentDefinition extends BeanMetadataElement {

	/**
	 * 获取这个{@code ComponentDefinition}的用户可见的名称.
	 * <p>这应该直接链接回给定上下文中该组件的相应配置数据.
	 */
	String getName();

	/**
	 * 返回所描述的组件的友好描述.
	 * <p>鼓励实现从{@code toString()}返回相同的值.
	 */
	String getDescription();

	/**
	 * 返回被注册到{@code ComponentDefinition}的{@link BeanDefinition BeanDefinitions}.
	 * <p>应该注意的是, {@code ComponentDefinition} 可能通过{@link BeanReference references}与其他{@link BeanDefinition BeanDefinitions}相关联,
	 * 但是这些不包括在内, 因为它们可能无法立即获得.
	 * 重要的{@link BeanReference BeanReferences}可以从 {@link #getBeanReferences()}获得.
	 * 
	 * @return BeanDefinition数组, 或空数组
	 */
	BeanDefinition[] getBeanDefinitions();

	/**
	 * 返回表示此组件中所有相关内部bean的{@link BeanDefinition BeanDefinitions}.
	 * <p>其他内部bean可能存在于关联的{@link BeanDefinition BeanDefinitions}中,
	 * 但是, 这些不被认为是验证或用户可视化所必需的.
	 * 
	 * @return BeanDefinition数组, 或空数组
	 */
	BeanDefinition[] getInnerBeanDefinitions();

	/**
	 * 返回被认为对此{@code ComponentDefinition}重要的{@link BeanReference BeanReferences}.
	 * <p>其他{@link BeanReference BeanReferences}可能存在于关联的{@link BeanDefinition BeanDefinitions}中,
	 * 但是, 这些不被认为是验证或用户可视化所必需的.
	 * 
	 * @return BeanDefinition数组, 或空数组
	 */
	BeanReference[] getBeanReferences();

}
