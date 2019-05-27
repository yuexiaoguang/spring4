package org.springframework.validation;

import java.io.Serializable;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * 基于Map的BindingResult接口实现, 支持在Map属性上注册和评估绑定错误.
 *
 * <p>可以用作自定义绑定到Map的错误持有者, 例如在为Map对象调用Validator时.
 */
@SuppressWarnings("serial")
public class MapBindingResult extends AbstractBindingResult implements Serializable {

	private final Map<?, ?> target;


	/**
	 * @param target 要绑定到的目标Map
	 * @param objectName 目标对象的名称
	 */
	public MapBindingResult(Map<?, ?> target, String objectName) {
		super(objectName);
		Assert.notNull(target, "Target Map must not be null");
		this.target = target;
	}


	public final Map<?, ?> getTargetMap() {
		return this.target;
	}

	@Override
	public final Object getTarget() {
		return this.target;
	}

	@Override
	protected Object getActualFieldValue(String field) {
		return this.target.get(field);
	}

}
