package org.springframework.messaging.simp.user;

/**
 * 当{@link java.security.Principal#getName() getName()}不是全局唯一的,
 * {@link java.security.Principal}也可以实现此约定, 因此不适合与"user"目标一起使用.
 */
public interface DestinationUserNameProvider {

	/**
	 * 返回全局唯一的用户名, 以便与"user"目标一起使用.
	 */
	String getDestinationUserName();

}
