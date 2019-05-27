package org.springframework.aop.framework;

/**
 * 标记接口，指示作为Spring的AOP基础结构的一部分的bean.
 * 特别是, 这意味着任何此类bean都不受自动代理的约束, 即使切点匹配.
 */
public interface AopInfrastructureBean {

}
