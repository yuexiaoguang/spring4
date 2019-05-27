package org.springframework.messaging.support;

import java.util.UUID;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.IdGenerator;

/**
 * 一个{@link org.springframework.messaging.support.MessageHeaderInitializer MessageHeaderInitializer},
 * 自定义ID和TIMESTAMP消息header生成的策略.
 */
public class IdTimestampMessageHeaderInitializer implements MessageHeaderInitializer {

	private IdGenerator idGenerator;

	private boolean enableTimestamp;


	/**
	 * 配置IdGenerator策略以初始化{@code MessageHeaderAccessor}实例.
	 * <p>默认{@code null}, 在这种情况下使用{@link org.springframework.messaging.MessageHeaders}的默认IdGenerator.
	 * <p>要完全没有生成ID, see {@link #setDisableIdGeneration()}.
	 */
	public void setIdGenerator(IdGenerator idGenerator) {
		this.idGenerator = idGenerator;
	}

	/**
	 * 返回已配置的{@code IdGenerator}.
	 */
	public IdGenerator getIdGenerator() {
		return this.idGenerator;
	}

	/**
	 * 使用id生成策略调用{@link #setIdGenerator}的快捷方式可以完全禁用id生成.
	 */
	public void setDisableIdGeneration() {
		this.idGenerator = ID_VALUE_NONE_GENERATOR;
	}

	/**
	 * 是否在正在初始化的{@code MessageHeaderAccessor}实例上
	 * 启用{@link org.springframework.messaging.MessageHeaders#TIMESTAMP} header的自动添加.
	 * <p>默认 false.
	 */
	public void setEnableTimestamp(boolean enableTimestamp) {
		this.enableTimestamp = enableTimestamp;
	}

	/**
	 * 返回是否启用timestamp header.
	 */
	public boolean isEnableTimestamp() {
		return this.enableTimestamp;
	}


	@Override
	public void initHeaders(MessageHeaderAccessor headerAccessor) {
		headerAccessor.setIdGenerator(getIdGenerator());
		headerAccessor.setEnableTimestamp(isEnableTimestamp());
	}


	private static final IdGenerator ID_VALUE_NONE_GENERATOR = new IdGenerator() {
		@Override
		public UUID generateId() {
			return MessageHeaders.ID_VALUE_NONE;
		}
	};

}
