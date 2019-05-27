/**
 * 核心Spring AOP接口, 建立在AOP Alliance AOP互操作性接口之上.
 *
 * <p>任何AOP Alliance MethodInterceptor都可以在Spring中使用.
 *
 * <br>Spring AOP还提供:
 * <ul>
 * <li>介绍支持
 * <li>切点抽象, 支持"static"切点 (基于类和方法)和 "dynamic"切点 (也考虑方法参数). 目前没有用于切点的AOP Alliance接口.
 * <li>所有的增强类型, 包括环绕, 前置, 后置, 返回, 抛出增强.
 * <li>可扩展性允许插入任意自定义增强类型, 而无需修改核心框架.
 * </ul>
 *
 * <p>Spring AOP可以以编程方式使用，或者（最好）与Spring IoC容器集成.
 */
package org.springframework.aop;
