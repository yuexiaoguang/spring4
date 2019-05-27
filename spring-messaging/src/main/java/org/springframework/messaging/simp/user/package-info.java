/**
 * 处理到"user"目标(i.e. 用户会话唯一的目标)的消息, 主要是转换目标, 然后将更新的消息转发给代理.
 *
 * <p>还包括{@link org.springframework.messaging.simp.user.SimpUserRegistry}, 用于跟踪连接的用户会话.
 */
package org.springframework.messaging.simp.user;
