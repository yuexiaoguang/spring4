/**
 * 通过HTTP调用器透明的Java-to-Java远程的远程类.
 * 像RMI一样使用Java序列化, 但提供与Caucho基于HTTP的Hessian和Burlap协议相同的易用性设置.
 *
 * <p><b>HTTP调用器是Java-to-Java远程处理的推荐协议.</b>
 * 它比Hessian和Burlap更强大, 更具扩展性, 但却牺牲了与Java的联系.
 * 尽管如此, 它与Hessian和Burlap一样容易设置, 这是它与RMI相比的主要优势.
 */
package org.springframework.remoting.httpinvoker;
