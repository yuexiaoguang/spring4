package org.springframework.web.servlet;

import java.util.HashMap;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * FlashMap为请求提供了一种存储属性, 然后在另一个请求中使用存储的属性的方法.
 * 当从一个URL重定向到另一个URL时, 最常需要这样做 -- e.g. Post/Redirect/Get 模式.
 * 在重定向之前保存FlashMap (通常在会话中), 并在重定向后立即可用并立即删除.
 *
 * <p>可以使用请求路径和请求参数设置FlashMap, 以帮助识别目标请求.
 * 如果没有此信息, FlashMap将可用于下一个请求, 该请求可能是也可能不是预期的接收者.
 * 在重定向上, 目标URL已知, 并且可以使用该信息更新FlashMap.
 * 使用{@code org.springframework.web.servlet.view.RedirectView}时会自动完成此操作.
 *
 * <p>Note: 带注解的控制器通常不会直接使用FlashMap.
 * 有关在带注解的控制器中使用Flash属性的概述, 请参阅{@code org.springframework.web.servlet.mvc.support.RedirectAttributes}.
 */
@SuppressWarnings("serial")
public final class FlashMap extends HashMap<String, Object> implements Comparable<FlashMap> {

	private String targetRequestPath;

	private final MultiValueMap<String, String> targetRequestParams = new LinkedMultiValueMap<String, String>(4);

	private long expirationTime = -1;


	/**
	 * 提供URL路径以帮助识别此FlashMap的目标请求.
	 * <p>路径可以是绝对的 (e.g. "/application/resource") 或相对于当前请求 (e.g. "../resource").
	 */
	public void setTargetRequestPath(String path) {
		this.targetRequestPath = path;
	}

	/**
	 * 返回目标URL路径 (或{@code null}).
	 */
	public String getTargetRequestPath() {
		return this.targetRequestPath;
	}

	/**
	 * 提供标识此FlashMap请求的请求参数.
	 * 
	 * @param params 带有预期参数名称和值的Map
	 */
	public FlashMap addTargetRequestParams(MultiValueMap<String, String> params) {
		if (params != null) {
			for (String key : params.keySet()) {
				for (String value : params.get(key)) {
					addTargetRequestParam(key, value);
				}
			}
		}
		return this;
	}

	/**
	 * 提供标识此FlashMap的请求的请求参数.
	 * 
	 * @param name 预期的参数名称 (如果为空或{@code null}则跳过)
	 * @param value 预期值 (如果为空或{@code null}则跳过)
	 */
	public FlashMap addTargetRequestParam(String name, String value) {
		if (StringUtils.hasText(name) && StringUtils.hasText(value)) {
			this.targetRequestParams.add(name, value);
		}
		return this;
	}

	/**
	 * 返回标识目标请求的参数或空Map.
	 */
	public MultiValueMap<String, String> getTargetRequestParams() {
		return this.targetRequestParams;
	}

	/**
	 * 启动此实例的到期时间.
	 * 
	 * @param timeToLive 过期时间, 以秒为单位
	 */
	public void startExpirationPeriod(int timeToLive) {
		this.expirationTime = System.currentTimeMillis() + timeToLive * 1000;
	}

	/**
	 * 设置FlashMap的到期时间.
	 * 这是为了序列化目的而提供的, 但也可以用来代替{@link #startExpirationPeriod(int)}.
	 */
	public void setExpirationTime(long expirationTime) {
		this.expirationTime = expirationTime;
	}

	/**
	 * 返回FlashMap的到期时间, 或-1.
	 */
	public long getExpirationTime() {
		return this.expirationTime;
	}

	/**
	 * 返回此实例是否已过期, 具体取决于调用{@link #startExpirationPeriod}后经过的时间量.
	 */
	public boolean isExpired() {
		return (this.expirationTime != -1 && System.currentTimeMillis() > this.expirationTime);
	}


	/**
	 * 比较两个FlashMaps, 更喜欢指定目标URL路径或具有更多目标URL参数的FlashMap.
	 * 在比较FlashMap实例之前, 确保它们与给定请求匹配.
	 */
	@Override
	public int compareTo(FlashMap other) {
		int thisUrlPath = (this.targetRequestPath != null ? 1 : 0);
		int otherUrlPath = (other.targetRequestPath != null ? 1 : 0);
		if (thisUrlPath != otherUrlPath) {
			return otherUrlPath - thisUrlPath;
		}
		else {
			return other.targetRequestParams.size() - this.targetRequestParams.size();
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof FlashMap)) {
			return false;
		}
		FlashMap otherFlashMap = (FlashMap) other;
		return (super.equals(otherFlashMap) &&
				ObjectUtils.nullSafeEquals(this.targetRequestPath, otherFlashMap.targetRequestPath) &&
				this.targetRequestParams.equals(otherFlashMap.targetRequestParams));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.targetRequestPath);
		result = 31 * result + this.targetRequestParams.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "FlashMap [attributes=" + super.toString() + ", targetRequestPath=" +
				this.targetRequestPath + ", targetRequestParams=" + this.targetRequestParams + "]";
	}

}
