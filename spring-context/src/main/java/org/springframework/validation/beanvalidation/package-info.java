/**
 * 支持类, 用于将JSR-303 Bean Validation提供程序 (例如Hibernate Validator)集成到Spring ApplicationContext中,
 * 特别是使用Spring的数据绑定和验证API.
 *
 * <p>中心类是{@link org.springframework.validation.beanvalidation.LocalValidatorFactoryBean},
 * 它为其他Spring组件的可用性定义了共享的ValidatorFactory/Validator设置.
 */
package org.springframework.validation.beanvalidation;
