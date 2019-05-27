package org.springframework.jmx.export.assembler;

/**
 * 扩展{@code MBeanInfoAssembler}以添加自动检测逻辑.
 * {@code MBeanExporter}为此接口的实现提供了在注册过程中包含其他bean的机会.
 *
 * <p>决定包含哪些bean的确切机制留给实现类.
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {

	/**
	 * 如果未在{@code MBeanExporter}的{@code beans}映射中指定, 则指示特定bean是否应包含在注册过程中.
	 * 
	 * @param beanClass bean的类 (可能是一个代理类)
	 * @param beanName bean工厂中bean的名称
	 */
	boolean includeBean(Class<?> beanClass, String beanName);

}
