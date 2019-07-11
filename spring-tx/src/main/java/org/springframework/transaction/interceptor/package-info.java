/**
 * 基于AOP的声明式事务划分解决方案.
 * 构建在org.springframework.aop.framework中的AOP基础结构上.
 * 任何POJO都可以通过Spring进行事务性增强.
 *
 * <p>TransactionFactoryProxyBean可用于为使用它们的代码透明地创建事务AOP代理.
 *
 * <p>TransactionInterceptor是AOP Alliance MethodInterceptor, 它基于Spring事务抽象提供事务增强.
 * 这允许在任何环境中进行声明式事务管理, 即使没有JTA, 如果应用程序仅使用单个数据库也是如此.
 */
package org.springframework.transaction.interceptor;
