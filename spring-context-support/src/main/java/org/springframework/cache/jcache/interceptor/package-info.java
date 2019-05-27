/**
 * 基于AOP的方案, 用于使用JSR-107注解进行声明性缓存划分.
 *
 * <p>基于org.springframework.cache.interceptor中的基础结构, 该基础结构处理Spring的缓存注解.
 *
 * <p>构建在org.springframework.aop.framework中的AOP基础结构上.
 * 任何POJO都可以通过Spring进行缓存增强.
 */
package org.springframework.cache.jcache.interceptor;
