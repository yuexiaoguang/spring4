package org.springframework.beans.factory.wiring;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link BeanWiringInfoResolver}接口的简单默认实现, 查找与完全限定类名相同的bean.
 * 如果未使用bean标签的“id”属性, 则匹配Spring XML文件中bean的默认名称.
 */
public class ClassNameBeanWiringInfoResolver implements BeanWiringInfoResolver {

	@Override
	public BeanWiringInfo resolveWiringInfo(Object beanInstance) {
		Assert.notNull(beanInstance, "Bean instance must not be null");
		return new BeanWiringInfo(ClassUtils.getUserClass(beanInstance).getName(), true);
	}

}
