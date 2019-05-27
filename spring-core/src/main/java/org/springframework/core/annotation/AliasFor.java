package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @AliasFor}是一个注解, 用于声明注解属性的别名.
 *
 * <h3>使用场景</h3>
 * <ul>
 * <li><strong>注解中的显式别名</strong>:
 * 在单个注解中, {@code @AliasFor}可以在一对属性上声明, 表示它们是彼此可互换的别名.</li>
 * 
 * <li><strong>元注解中属性的显式别名</strong>:
 * 如果{@code @AliasFor}的{@link #annotation}属性设置为与声明它的注解不同的注解,
 * 则{@link #attribute}将被解释为元注解中属性的别名 (i.e., 显式的元注释属性覆盖).
 * 这样可以精确控制在注解层次结构中覆盖哪些属性.
 * 实际上, 使用{@code @AliasFor}, 甚至可以为元注解的{@code value}属性声明别名.</li>
 * 
 * <li><strong>注解中的隐式别名</strong>:
 * 如果注解中的一个或多个属性被声明为同一元注解属性的属性覆盖 (直接或可传递),
 * 这些属性将被视为彼此的一组<em>隐式</em>别名, 导致行为类似于注解中显式别名的行为.</li>
 * </ul>
 *
 * <h3>使用要求</h3>
 * <p>与Java中的任何注解一样, 单独存在{@code @AliasFor}将不会强制别名语义.
 * 要强制执行别名语义, 必须通过{@link AnnotationUtils}中的实用程序方法<em>加载</em>注解.
 * 在幕后, Spring将<em>合成</em>注解, 通过将其包装在动态代理中,
 * 透明地为带有{@code @AliasFor}注解的注解属性强制执行<em>属性别名</em>语义.
 * 同样, {@link AnnotatedElementUtils}支持在注解层次结构中使用{@code @AliasFor}时显式的元注解属性覆盖.
 * 通常, 您不需要自己手动合成注解, 因为当在Spring管理的组件上查找注解时, Spring会透明地为您执行此操作.
 *
 * <h3>实现要求</h3>
 * <ul>
 * <li><strong>注解中的显式别名</strong>:
 * <ol>
 * <li>构成别名对的每个属性都必须使用{@code @AliasFor}进行注解, 并且{@link #attribute}或{@link #value}必须引用该对中的<em>其他</em>属性.</li>
 * <li>别名属性必须声明相同的返回类型.</li>
 * <li>别名属性必须声明默认值.</li>
 * <li>别名属性必须声明相同的默认值.</li>
 * <li>不应声明{@link #annotation}.</li>
 * </ol>
 * </li>
 * <li><strong>元注解中属性的显式别名</strong>:
 * <ol>
 * <li>作为元注解中属性的别名的属性必须使用{@code @AliasFor}进行注解, 并且{@link #attribute}必须在元注解中引用该属性.</li>
 * <li>别名属性必须声明相同的返回类型.</li>
 * <li>{@link #annotation}必须引用元注解.</li>
 * <li>引用的元注解必须是声明{@code @AliasFor}的注解类上的<em>元存在</em>.</li>
 * </ol>
 * </li>
 * <li><strong>注解中的隐式别名</strong>:
 * <ol>
 * <li>属于一组隐式别名的每个属性必须使用{@code @AliasFor}进行注解, 并且{@link #attribute}必须在同一元注解中引用相同的属性
 * (直接或传递通过注解层次结构中的其他显式元注解属性覆盖).</li>
 * <li>别名属性必须声明相同的返回类型.</li>
 * <li>别名属性必须声明默认值.</li>
 * <li>别名属性必须声明相同的默认值.</li>
 * <li>{@link #annotation}必须引用适当的元注解.</li>
 * <li>引用的元注解必须是声明{@code @AliasFor}的注解类上的<em>元存在</em>.</li>
 * </ol>
 * </li>
 * </ul>
 *
 * <h3>Example: 注解中的显式别名</h3>
 * <p>在{@code @ContextConfiguration}中, {@code value}和{@code locations}是彼此的显式别名.
 *
 * <pre class="code"> public &#064;interface ContextConfiguration {
 *
 *    &#064;AliasFor("locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor("value")
 *    String[] locations() default {};
 *
 *    // ...
 * }</pre>
 *
 * <h3>Example: 元注解中属性的显式别名</h3>
 * <p>在{@code @XmlTestConfig}中, {@code xmlFiles}是{@code @ContextConfiguration}中{@code locations}的显式别名.
 * 换句话说, {@code xmlFiles}会覆盖{@code @ContextConfiguration}中的{@code locations}属性.
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface XmlTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles();
 * }</pre>
 *
 * <h3>Example: 注解中的隐式别名</h3>
 * <p>在{@code @MyTestConfig}中, {@code value}, {@code groovyScripts}, 和{@code xmlFiles}
 * 都是{@code @ContextConfiguration}中{@code locations}属性的显式元注解属性覆盖.
 * 因此, 这三个属性也是彼此的隐式别名.
 *
 * <pre class="code"> &#064;ContextConfiguration
 * public &#064;interface MyTestConfig {
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] value() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] groovyScripts() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xmlFiles() default {};
 * }</pre>
 *
 * <h3>Example: 注解中的传递隐式别名</h3>
 * <p>在{@code @GroovyOrXmlTestConfig}中, {@code groovy}是{@code @MyTestConfig}中{@code groovyScripts}属性的显式覆盖;
 * 而{@code xml}是{@code @ContextConfiguration}中{@code locations}属性的显式覆盖.
 * 此外, {@code groovy}和{@code xml}是彼此的传递隐式别名, 因为它们都有效地覆盖{@code @ContextConfiguration}中的{@code locations}属性.
 *
 * <pre class="code"> &#064;MyTestConfig
 * public &#064;interface GroovyOrXmlTestConfig {
 *
 *    &#064;AliasFor(annotation = MyTestConfig.class, attribute = "groovyScripts")
 *    String[] groovy() default {};
 *
 *    &#064;AliasFor(annotation = ContextConfiguration.class, attribute = "locations")
 *    String[] xml() default {};
 * }</pre>
 *
 * <h3>支持属性别名的Spring注解</h3>
 * <p>从Spring Framework 4.2开始, 核心Spring中的几个注解已更新为使用{@code @AliasFor}来配置其内部属性别名.
 * 有关详细信息, 请参阅Javadoc以获取个别注释以及参考手册.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface AliasFor {

	/**
	 * {@link #attribute}的别名.
	 * <p>当{@link #annotation}未声明时, 打算用来代替{@link #attribute}&mdash;
	 * 例如: {@code @AliasFor("value")} 而不是{@code @AliasFor(attribute = "value")}.
	 */
	@AliasFor("attribute")
	String value() default "";

	/**
	 * <em>此</em>属性是其别名的属性名称.
	 */
	@AliasFor("value")
	String attribute() default "";

	/**
	 * 声明别名{@link #attribute}的注解类型.
	 * <p>默认{@link Annotation}, 暗示别名属性在与<em>此</em>属性相同的注解中声明.
	 */
	Class<? extends Annotation> annotation() default Annotation.class;

}
