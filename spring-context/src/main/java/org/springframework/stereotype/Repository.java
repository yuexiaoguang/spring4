package org.springframework.stereotype;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示带注解的类是"Repository", 最初由Domain-Driven Design (Evans, 2003)定义为
 * "用于封装存储, 检索和搜索行为的机制, 用于模拟对象集合".
 *
 * <p>实现传统Java EE模式, 如 "Data Access Object", 的团队也可以将此构造型应用于DAO类,
 * 虽然在这样做之前应该注意理解数据访问对象和DDD样式存储库之间的区别.
 * 这个注释是一个通用的构造型, 个别团队可能会缩小其语义并在适当时使用.
 *
 * <p>这样被注解的类符合Spring {@link org.springframework.dao.DataAccessException DataAccessException}转换的条件,
 * 当与{@link org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor}一起使用时.
 * 为了工具, 切面等目的, 注解类还阐明了它在整个应用程序体系结构中的作用.
 *
 * <p>从Spring 2.5开始, 这个注解也可以作为{@link Component @Component}的专门化, 允许通过类路径扫描自动检测实现类.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Repository {

	/**
	 * 该值可以指示对逻辑组件名称的建议, 在自动检测组件时将其转换为Spring bean.
	 * 
	 * @return 建议的组件名称 (否则为空字符串)
	 */
	String value() default "";

}
