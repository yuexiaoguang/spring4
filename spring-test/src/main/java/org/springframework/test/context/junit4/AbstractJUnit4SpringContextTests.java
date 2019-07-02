package org.springframework.test.context.junit4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * 抽象基本测试类, 它将<em>Spring TestContext Framework</em>
 * 与<strong>JUnit</strong>环境中的显式{@link ApplicationContext}测试支持集成在一起.
 *
 * <p>具体的子类通常应声明一个类级{@link ContextConfiguration @ContextConfiguration}注解来配置
 * {@linkplain ApplicationContext 应用程序上下文} {@link ContextConfiguration#locations() 资源位置}
 * 或 {@link ContextConfiguration#classes() 带注解的类}.
 * <em>如果测试不需要加载应用程序上下文, 可以选择省略{@link ContextConfiguration @ContextConfiguration}声明,
 * 并手动配置相应的{@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}.</em>
 *
 * <p>默认情况下配置以下{@link org.springframework.test.context.TestExecutionListener TestExecutionListeners}:
 *
 * <ul>
 * <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 * <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * </ul>
 *
 * <p>此类仅用作扩展的便利.
 * <ul>
 * <li>如果不希望将测试类绑定到特定于Spring的类层次结构, 则可以使用
 * {@link SpringRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}等配置自己的自定义测试类.</li>
 * <li>如果想扩展这个类并使用{@link SpringRunner}以外的运行器, 从Spring Framework 4.2开始, 可以使用
 * {@link org.springframework.test.context.junit4.rules.SpringClassRule SpringClassRule}
 * 和{@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule},
 * 并通过{@link RunWith @RunWith(...)}指定你选择的运行器.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> 从Spring Framework 4.3开始, 此类需要JUnit 4.12或更高版本.
 */
@RunWith(SpringRunner.class)
@TestExecutionListeners({ ServletTestExecutionListener.class, DirtiesContextBeforeModesTestExecutionListener.class,
	DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
public abstract class AbstractJUnit4SpringContextTests implements ApplicationContextAware {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 通过{@link #setApplicationContext(ApplicationContext)}注入此测试实例的{@link ApplicationContext}.
	 */
	protected ApplicationContext applicationContext;


	/**
	 * 设置此测试实例使用的{@link ApplicationContext}, 通过{@link ApplicationContextAware}语义提供.
	 * 
	 * @param applicationContext 此测试运行的ApplicationContext
	 */
	@Override
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
