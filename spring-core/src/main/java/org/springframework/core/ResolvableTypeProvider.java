package org.springframework.core;

/**
 * 任何对象都可以实现此接口以提供其实际的{@link ResolvableType}.
 *
 * <p>在确定实例是否与泛型签名匹配时, 此类信息非常有用, 因为Java在运行时未传达签名.
 *
 * <p>在复杂的层次结构场景中, 此接口的用户应该小心, 尤其是当类的泛型类型签名在子类中更改时.
 * 始终可以返回{@code null}以回退默认行为.
 */
public interface ResolvableTypeProvider {

	/**
	 * 返回描述此实例的{@link ResolvableType} (或{@code null} 如果应该应用某种默认值).
	 */
	ResolvableType getResolvableType();

}
