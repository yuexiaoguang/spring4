package org.springframework.messaging.simp.user;

import java.util.concurrent.ScheduledFuture;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * {@code MessageHandler}, 用于处理来自其他应用程序服务器的用户注册表广播, 并定期广播本地用户注册表的内容.
 *
 * <p>汇总信息保存在{@link MultiServerUserRegistry}中.
 */
public class UserRegistryMessageHandler implements MessageHandler, ApplicationListener<BrokerAvailabilityEvent> {

	private final MultiServerUserRegistry userRegistry;

	private final SimpMessagingTemplate brokerTemplate;

	private final String broadcastDestination;

	private final TaskScheduler scheduler;

	private final UserRegistryTask schedulerTask = new UserRegistryTask();

	private volatile ScheduledFuture<?> scheduledFuture;

	private long registryExpirationPeriod = 20 * 1000;


	/**
	 * @param userRegistry 具有本地和远程用户注册表信息的注册表
	 * @param brokerTemplate 用于广播本地注册信息的模板
	 * @param broadcastDestination 要广播到的目标
	 * @param scheduler 要使用的任务定时器
	 */
	public UserRegistryMessageHandler(MultiServerUserRegistry userRegistry,
			SimpMessagingTemplate brokerTemplate, String broadcastDestination, TaskScheduler scheduler) {

		Assert.notNull(userRegistry, "'userRegistry' is required");
		Assert.notNull(brokerTemplate, "'brokerTemplate' is required");
		Assert.hasText(broadcastDestination, "'broadcastDestination' is required");
		Assert.notNull(scheduler, "'scheduler' is required");

		this.userRegistry = userRegistry;
		this.brokerTemplate = brokerTemplate;
		this.broadcastDestination = broadcastDestination;
		this.scheduler = scheduler;
	}


	/**
	 * 返回用于广播UserRegistry信息的配置的目标.
	 */
	public String getBroadcastDestination() {
		return this.broadcastDestination;
	}

	/**
	 * 配置远程用户注册表快照被视为过期之前的时间 (以毫秒为单位).
	 * <p>默认20 seconds (value of 20000).
	 * 
	 * @param milliseconds 过期时间
	 */
	@SuppressWarnings("unused")
	public void setRegistryExpirationPeriod(long milliseconds) {
		this.registryExpirationPeriod = milliseconds;
	}

	/**
	 * 返回配置的过期时间.
	 */
	public long getRegistryExpirationPeriod() {
		return this.registryExpirationPeriod;
	}


	@Override
	public void onApplicationEvent(BrokerAvailabilityEvent event) {
		if (event.isBrokerAvailable()) {
			long delay = getRegistryExpirationPeriod() / 2;
			this.scheduledFuture = this.scheduler.scheduleWithFixedDelay(this.schedulerTask, delay);
		}
		else {
			ScheduledFuture<?> future = this.scheduledFuture;
			if (future != null ){
				future.cancel(true);
				this.scheduledFuture = null;
			}
		}
	}

	@Override
	public void handleMessage(Message<?> message) throws MessagingException {
		MessageConverter converter = this.brokerTemplate.getMessageConverter();
		this.userRegistry.addRemoteRegistryDto(message, converter, getRegistryExpirationPeriod());
	}


	private class UserRegistryTask implements Runnable {

		@Override
		public void run() {
			try {
				SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
				accessor.setHeader(SimpMessageHeaderAccessor.IGNORE_ERROR, true);
				accessor.setLeaveMutable(true);
				Object payload = userRegistry.getLocalRegistryDto();
				brokerTemplate.convertAndSend(getBroadcastDestination(), payload, accessor.getMessageHeaders());
			}
			finally {
				userRegistry.purgeExpiredRegistries();
			}
		}
	}

}
