package org.springframework.context.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指示当有多个候选者可以自动装配单个值的依赖项时, 应优先考虑的bean.
 * 如果候选者中只存在一个'主' bean, 则它将是自动装配的值.
 *
 * <p>此注解在语义上等同于Spring XML中的{@code <bean>}元素的{@code primary}属性.
 *
 * <p>可以在直接或间接使用{@code @Component}注解的类或使用 @{@link Bean}注解的方法上使用.
 *
 * <h2>Example</h2>
 * <pre class="code">
 * &#064;Component
 * public class FooService {
 *
 *     private FooRepository fooRepository;
 *
 *     &#064;Autowired
 *     public FooService(FooRepository fooRepository) {
 *         this.fooRepository = fooRepository;
 *     }
 * }
 *
 * &#064;Component
 * public class JdbcFooRepository extends FooRepository {
 *
 *     public JdbcFooRepository(DataSource dataSource) {
 *         // ...
 *     }
 * }
 *
 * &#064;Primary
 * &#064;Component
 * public class HibernateFooRepository extends FooRepository {
 *
 *     public HibernateFooRepository(SessionFactory sessionFactory) {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>因为{@code HibernateFooRepository}标有{@code @Primary},
 * 它将优先注入基于jdbc的变体, 假设两者在相同的Spring应用程序上下文中作为bean存在, 这通常是在组件扫描被大量应用的情况下.
 *
 * <p>请注意, 除非正在使用组件扫描, 否则在类级别使用{@code @Primary}无效.
 * 如果通过XML声明了带{@code @Primary}注解的类, 则忽略{@code @Primary}注解元数据, 使用{@code <bean primary="true|false"/>}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Primary {

}
