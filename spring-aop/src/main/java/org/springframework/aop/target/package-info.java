/**
 * 该包包含org.springframework.aop.TargetSource接口的实现.
 * <br>
 * 最简单的实现是SingletonTargetSource, 默认情况下在AOP框架中用于包装单个目标实例. 这通常是合适的.
 * 
 * <br>
 * 其他提供的实现包括池实现, 为每个请求提供池中的目标, 确保单线程编程模型;
 * 和一个"prototype"实现, 每次调用都使用一个新的目标实例.
 */
package org.springframework.aop.target;
