package org.springframework.test.context.junit4;

import org.junit.runners.model.InitializationError;

/**
 * {@code SpringRunner}是{@link SpringJUnit4ClassRunner}的<em>别名</em>.
 *
 * <p>要使用此类, 只需使用{@code @RunWith(SpringRunner.class)}注解基于JUnit 4的测试类.
 *
 * <p>如果您想将Spring TestContext Framework与除此之外的运行器一起使用,
 * 使用{@link org.springframework.test.context.junit4.rules.SpringClassRule}
 * 和{@link org.springframework.test.context.junit4.rules.SpringMethodRule}.
 *
 * <p><strong>NOTE:</strong> 此类需要JUnit 4.12或更高版本.
 */
public final class SpringRunner extends SpringJUnit4ClassRunner {

	/**
	 * 构造一个新的{@code SpringRunner}, 并初始化一个
	 * {@link org.springframework.test.context.TestContextManager TestContextManager}, 
	 * 为标准的JUnit 4测试提供Spring测试功能.
	 * 
	 * @param clazz 要运行的测试类
	 */
	public SpringRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
	}

}
