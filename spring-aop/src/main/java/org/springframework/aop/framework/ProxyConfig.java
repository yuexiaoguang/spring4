package org.springframework.aop.framework;

import java.io.Serializable;

import org.springframework.util.Assert;

/**
 * 用于创建代理的配置的便捷超类, 确保所有代理创建者都具有一致的属性.
 */
public class ProxyConfig implements Serializable {

	/** use serialVersionUID from Spring 1.2 for interoperability */
	private static final long serialVersionUID = -8409359707199703185L;


	private boolean proxyTargetClass = false;

	private boolean optimize = false;

	boolean opaque = false;

	boolean exposeProxy = false;

	private boolean frozen = false;


	/**
	 * 设置是否直接代理目标类, 而不只是代理特定的接口.
	 * 默认是 "false".
	 * <p>设置为 "true", 强制代理TargetSource的公开目标类.
	 * 如果该目标类是接口, 将为给定的接口创建JDK代理. 如果该目标类是任何其他类, 将为给定的类创建CGLIB代理.
	 * <p>Note: 取决于具体代理工厂的配置, 如果未指定任何接口, 则还将应用proxy-target-class行为 (并且没有激活接口自动检测).
	 */
	public void setProxyTargetClass(boolean proxyTargetClass) {
		this.proxyTargetClass = proxyTargetClass;
	}

	/**
	 * 返回是否直接代理目标类以及任何接口.
	 */
	public boolean isProxyTargetClass() {
		return this.proxyTargetClass;
	}

	/**
	 * 设置代理是否应执行积极优化.
	 * 代理之间“积极优化”的确切含义会有所不同, 但通常有一些权衡.
	 * 默认是 "false".
	 * <p>例如, 优化通常意味着在创建代理后, 增强更改不会生效. 为此, 默认情况下禁用优化.
	 * 如果其他设置排除优化，则可以忽略优化值 "true": 例如, 如果"exposeProxy"设置为"true", 这与优化不兼容.
	 */
	public void setOptimize(boolean optimize) {
		this.optimize = optimize;
	}

	/**
	 * 返回代理是否应执行积极的优化.
	 */
	public boolean isOptimize() {
		return this.optimize;
	}

	/**
	 * 设置是否应阻止将此配置创建的代理强制转换为{@link Advised}, 以查询代理状态.
	 * <p>默认是 "false", 意味着可以将任何AOP代理转换为{@link Advised}.
	 */
	public void setOpaque(boolean opaque) {
		this.opaque = opaque;
	}

	/**
	 * 返回是否应该阻止此配置创建的代理被强制转换为{@link Advised}.
	 */
	public boolean isOpaque() {
		return this.opaque;
	}

	/**
	 * 设置代理是否应该由AOP框架作为ThreadLocal公开, 以便通过AopContext类进行检索.
	 * 如果增强的对象需要自己调用另一个增强的方法，这将非常有用. (如果它使用 {@code this}, 不会增强调用).
	 * <p>默认是 "false", 为了避免不必要的额外拦截.
	 * 这意味着不保证, AopContext访问将在增强的对象的方法中一致地工作.
	 */
	public void setExposeProxy(boolean exposeProxy) {
		this.exposeProxy = exposeProxy;
	}

	/**
	 * 返回AOP代理是否将为每次调用公开AOP代理.
	 */
	public boolean isExposeProxy() {
		return this.exposeProxy;
	}

	/**
	 * 设置是否应冻结此配置.
	 * <p>配置冻结时, 不能改变增强. 这对优化很有用, 当我们不希望调用者在转换为Advised之后继续操作配置时, 这很有用.
	 */
	public void setFrozen(boolean frozen) {
		this.frozen = frozen;
	}

	/**
	 * 返回是否冻结配置, 不能改变增强.
	 */
	public boolean isFrozen() {
		return this.frozen;
	}


	/**
	 * 从其他配置对象复制配置.
	 * 
	 * @param other 从中复制配置的对象
	 */
	public void copyFrom(ProxyConfig other) {
		Assert.notNull(other, "Other ProxyConfig object must not be null");
		this.proxyTargetClass = other.proxyTargetClass;
		this.optimize = other.optimize;
		this.exposeProxy = other.exposeProxy;
		this.frozen = other.frozen;
		this.opaque = other.opaque;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("proxyTargetClass=").append(this.proxyTargetClass).append("; ");
		sb.append("optimize=").append(this.optimize).append("; ");
		sb.append("opaque=").append(this.opaque).append("; ");
		sb.append("exposeProxy=").append(this.exposeProxy).append("; ");
		sb.append("frozen=").append(this.frozen);
		return sb.toString();
	}
}
