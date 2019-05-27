package org.springframework.cglib.core;

/**
 * CGLIB的{@link DefaultNamingPolicy}自定义扩展, 将生成的类名中的标签从"ByCGLIB" 修改为 "BySpringCGLIB".
 *
 * <p>这主要是为了避免常规CGLIB版本 (由其他一些库使用)和Spring的嵌入式变体之间的冲突, 以防代理同一个类用于不同的目的.
 */
public class SpringNamingPolicy extends DefaultNamingPolicy {

	public static final SpringNamingPolicy INSTANCE = new SpringNamingPolicy();

	@Override
	protected String getTag() {
		return "BySpringCGLIB";
	}

}
