package org.springframework.context;

/**
 * 可能参与分阶段过程(如生命周期管理)的对象的接口.
 */
public interface Phased {

	/**
	 * 返回此对象的阶段值.
	 */
	int getPhase();

}
