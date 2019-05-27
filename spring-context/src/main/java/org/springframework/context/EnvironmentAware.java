package org.springframework.context;

import org.springframework.beans.factory.Aware;
import org.springframework.core.env.Environment;

/**
 * 希望被通知其运行的{@link Environment}的bean实现的接口.
 */
public interface EnvironmentAware extends Aware {

	/**
	 * 设置此组件运行的{@code Environment}.
	 */
	void setEnvironment(Environment environment);

}
