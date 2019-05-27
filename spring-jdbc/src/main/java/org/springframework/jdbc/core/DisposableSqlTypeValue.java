package org.springframework.jdbc.core;

/**
 * 添加清理回调的{@link SqlTypeValue}的子接口, 在设置值并执行相应的语句后调用.
 */
public interface DisposableSqlTypeValue extends SqlTypeValue {

	/**
	 * 清除此类型值所持有的资源, 例如SqlLobValue时的LobCreator.
	 */
	void cleanup();

}
