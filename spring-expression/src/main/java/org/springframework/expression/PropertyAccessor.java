package org.springframework.expression;


/**
 * 属性访问器能够读取 (并可能写入)对象的属性.
 * 此接口没有任何限制, 因此实现者可以直接作为字段, 或通过getter, 或以他们认为合适的任何其他方式直接访问属性.
 *
 * <p>解析器可以选择指定应该为其调用的目标类数组.
 * 但是, 如果它从{@link #getSpecificTargetClasses()}返回{@code null}, 将调用它用于所有属性引用,
 * 并有机会确定它是否可以读取或写入它们.
 *
 * <p>属性解析器被认为是有序的, 每个都将依次调用.
 * 影响调用顺序的唯一规则是, 在一般解析器之前, 将首先调用直接在{@link #getSpecificTargetClasses()}中的命名目标类.
 */
public interface PropertyAccessor {

	/**
	 * 返回应该调用此解析器的类的数组.
	 * <p>>返回{@code null}表示这是一个通用解析程序, 可以在尝试解析任何类型的属性时调用它.
	 * 
	 * @return 此解析器适合的类的数组 (或{@code null}, 如果是通用解析器)
	 */
	Class<?>[] getSpecificTargetClasses();

	/**
	 * 解析器实例是否能够访问指定目标对象上的指定属性.
	 * 
	 * @param context 要尝试访问的评估上下文
	 * @param target 要访问该属性的目标对象
	 * @param name 要访问的属性的名称
	 * 
	 * @return true 如果此解析器能够读取该属性
	 * @throws AccessException 确定是否可以访问该属性时有问题
	 */
	boolean canRead(EvaluationContext context, Object target, String name) throws AccessException;

	/**
	 * 从指定的目标对象读取属性.
	 * 只有{@link #canRead}也返回{@code true}时才会成功.
	 * 
	 * @param context 要尝试访问的评估上下文
	 * @param target 要访问该属性的目标对象
	 * @param name 要访问的属性的名称
	 * 
	 * @return 包装读取的属性值及其类型描述符的TypedValue对象
	 * @throws AccessException 访问该属性值时有问题
	 */
	TypedValue read(EvaluationContext context, Object target, String name) throws AccessException;

	/**
	 * 解析器实例是否能够写入指定目标对象上的指定属性.
	 * 
	 * @param context 要尝试访问的评估上下文
	 * @param target 要访问该属性的目标对象
	 * @param name 要访问的属性的名称
	 * 
	 * @return true 如果此解析器能够写入属性
	 * @throws AccessException 确定是否可以写入属性时有问题
	 */
	boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException;

	/**
	 * 写入指定目标对象上的属性.
	 * 只有{@link #canWrite}也返回{@code true}才能成功.
	 * 
	 * @param context 要尝试访问的评估上下文
	 * @param target 要访问该属性的目标对象
	 * @param name 要访问的属性的名称
	 * @param newValue 属性的新值
	 * 
	 * @throws AccessException 写入属性值时有问题
	 */
	void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException;

}
