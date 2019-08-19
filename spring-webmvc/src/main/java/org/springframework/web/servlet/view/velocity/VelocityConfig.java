package org.springframework.web.servlet.view.velocity;

import org.apache.velocity.app.VelocityEngine;

/**
 * 由配置和管理VelocityEngine以在Web环境中自动查找的对象实现的接口.
 * 由VelocityView检测并使用.
 *
 * @deprecated 从Spring 4.3开始, 支持FreeMarker
 */
@Deprecated
public interface VelocityConfig {

	/**
	 * Return the VelocityEngine for the current web application context.
	 * May be unique to one servlet, or shared in the root context.
	 * @return the VelocityEngine
	 */
	VelocityEngine getVelocityEngine();

}
