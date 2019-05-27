package org.springframework.aop;

/**
 * 用于抛出增强的标记接口.
 *
 * <p>此接口上没有任何方法, 因为反射调用了方法. 实现类必须实现按以下形式实现方法:
 *
 * <pre class="code">void afterThrowing([Method, args, target], ThrowableSubclass);</pre>
 *
 * <p>有效方法的一些例子是:
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * 前三个参数是可选的, 并且仅在我们想要有关连接点的更多信息时才有用, 就像 AspectJ <b>after-throwing</b>增强.
 *
 * <p><b>Note:</b>如果throws-advice方法本身抛出异常, 它将覆盖原始异常 (i.e. 修改抛出给用户的异常).
 * 覆盖异常通常是RuntimeException; 这与所有方法签名兼容.
 * 但是, 如果throws-advice方法抛出一个受检异常, 它必须匹配目标方法声明的异常, 因此在某种程度上耦合到特定的目标方法签名.
 * <b>不要抛出与目标方法的签名不兼容的未声明的受检异常!</b>
 */
public interface ThrowsAdvice extends AfterAdvice {

}
