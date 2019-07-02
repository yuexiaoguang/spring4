package org.springframework.test.context;

import java.util.List;

/**
 * {@code TestContextBootstrapper}定义用于引导<em>Spring TestContext Framework</em>的SPI.
 *
 * <p>{@code TestContextBootstrapper}被{@link TestContextManager}用于
 * {@linkplain #getTestExecutionListeners 获取当前测试的TestExecutionListeners},
 * 和{@linkplain #buildTestContext 构建它管理的TestContext}.
 *
 * <h3>配置</h3>
 *
 * <p>可以通过{@link BootstrapWith @BootstrapWith}直接或作为元注解为测试类(或测试类层次结构)配置自定义引导策略.
 *
 * <p>如果未通过{@code @BootstrapWith}显式配置引导程序,
 * 则将使用
 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper DefaultTestContextBootstrapper}
 * 或{@link org.springframework.test.context.web.WebTestContextBootstrapper WebTestContextBootstrapper},
 * 具体取决于
 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}的存在.
 *
 * <h3>实现注意</h3>
 *
 * <p>具体实现必须提供{@code public}无参构造函数.
 *
 * <p><strong>WARNING</strong>: 这个SPI将来可能会发生变化, 以满足新的要求.
 * 因此强烈建议实现者<strong>不</strong>直接实现此接口, 而是<em>扩展</em>
 * {@link org.springframework.test.context.support.AbstractTestContextBootstrapper AbstractTestContextBootstrapper}
 * 或其具体子类之一.
 */
public interface TestContextBootstrapper {

	/**
	 * 设置此引导程序使用的{@link BootstrapContext}.
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

	/**
	 * 获取与此引导程序关联的{@link BootstrapContext}.
	 */
	BootstrapContext getBootstrapContext();

	/**
	 * 为与此引导程序关联的{@link BootstrapContext}构建{@link TestContext}.
	 * 
	 * @return 新的{@link TestContext}, never {@code null}
	 */
	TestContext buildTestContext();

	/**
	 * 在与此引导程序关联的{@link BootstrapContext}中为测试类构建{@linkplain MergedContextConfiguration 合并上下文配置}.
	 * <p>在构建合并配置时, 实现必须考虑以下因素:
	 * <ul>
	 * <li>通过{@link ContextHierarchy @ContextHierarchy}和{@link ContextConfiguration @ContextConfiguration}声明的上下文层次结构</li>
	 * <li>通过{@link ActiveProfiles @ActiveProfiles}声明的活动bean定义配置文件</li>
	 * <li>通过{@link ContextConfiguration#initializers}声明的
	 * {@linkplain org.springframework.context.ApplicationContextInitializer 上下文初始化器}</li>
	 * <li>通过{@link TestPropertySource @TestPropertySource}声明的测试属性源</li>
	 * </ul>
	 * <p>有关所需语义的详细信息, 请参阅上述注解Javadoc.
	 * <p>请注意, {@link #buildTestContext()}的实现通常应在构造{@code TestContext}时委托给此方法.
	 * <p>在确定要用于给定测试类的{@link ContextLoader}时, 应使用以下算法:
	 * <ol>
	 * <li>如果已通过{@link ContextConfiguration#loader}显式声明了{@code ContextLoader}类, 请使用它.</li>
	 * <li>否则, 具体实现可以自由确定哪个{@code ContextLoader}类用作默认值.</li>
	 * </ol>
	 * 
	 * @return 合并的上下文配置, never {@code null}
	 */
	MergedContextConfiguration buildMergedContextConfiguration();

	/**
	 * 获取与此引导程序关联的{@link BootstrapContext}中的测试类的新实例化的{@link TestExecutionListener TestExecutionListeners}的列表.
	 * <p>如果{@code BootstrapContext}中的测试类上的{@link TestExecutionListeners @TestExecutionListeners}<em>不存在</em>,
	 * 则应返回<em>默认</em>监听器.
	 * 此外, 必须使用
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator AnnotationAwareOrderComparator}
	 * 对默认监听器进行排序.
	 * <p>具体实现可以自由地确定包含默认监听器集合的内容.
	 * 但是, 默认情况下, Spring TestContext Framework将使用
	 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * 机制查找在类路径上的所有{@code META-INF/spring.factories}文件中配置的所有{@code TestExecutionListener}类名.
	 * <p>必须考虑{@link TestExecutionListeners @TestExecutionListeners}的
	 * {@link TestExecutionListeners#inheritListeners() inheritListeners}标志.
	 * 具体来说, 如果{@code inheritListeners}标志设置为{@code true},
	 * 则为给定测试类声明的监听器必须附加到超类中声明的监听器列表的末尾.
	 * 
	 * @return {@code TestExecutionListener}实例列表
	 */
	List<TestExecutionListener> getTestExecutionListeners();

}
