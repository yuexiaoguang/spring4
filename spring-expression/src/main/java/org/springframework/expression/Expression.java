package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;

/**
 * 能够根据上下文对象评估自身的表达式.
 * 封装先前解析的表达式字符串的详细信息.
 * 为表达式评估提供通用抽象.
 */
public interface Expression {

	/**
	 * 返回用于创建此表达式的原始字符串 (未修改).
	 * 
	 * @return 原始表达式字符串
	 */
	String getExpressionString();

	/**
	 * 在默认标准上下文中评估此表达式.
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	Object getValue() throws EvaluationException;

	/**
	 * 在默认上下文中计算表达式.
	 * 如果评估结果与预期结果类型不匹配 (并且无法转换为), 则将返回异常.
	 * 
	 * @param desiredResultType 调用者期望的结果类型
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	<T> T getValue(Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 根据指定的根对象计算此表达式.
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	Object getValue(Object rootObject) throws EvaluationException;

	/**
	 * 根据指定的根对象评估默认上下文中的表达式.
	 * 如果评估结果与预期结果类型不匹配 (并且无法转换为), 则将返回异常.
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * @param desiredResultType 调用者期望的结果类型
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	<T> T getValue(Object rootObject, Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 在提供的上下文中评估此表达式, 并返回评估结果.
	 * 
	 * @param context 评估表达式的上下文
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	Object getValue(EvaluationContext context) throws EvaluationException;

	/**
	 * 在提供的上下文中评估此表达式并返回评估结果, 但使用提供的根上下文作为上下文中指定的任何默认根对象的覆盖.
	 * 
	 * @param context 评估表达式的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	Object getValue(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * 评估指定上下文中的表达式, 该表达式可以解析对属性, 方法, 类型等的引用.
	 * 评估结果的类型应该是特定类, 如果不是并且不能转换为该类型, 则抛出异常.
	 * 
	 * @param context 评估表达式的上下文
	 * @param desiredResultType 调用者期望的结果类型
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	<T> T getValue(EvaluationContext context, Class<T> desiredResultType) throws EvaluationException;

	/**
	 * 评估指定上下文中的表达式, 该表达式可以解析对属性, 方法, 类型等的引用.
	 * 评估结果的类型应该是特定类, 如果不是并且不能转换为该类型, 则抛出异常.
	 * 提供的根对象将覆盖在提供的上下文中指定的任何默认值.
	 * 
	 * @param context 评估表达式的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * @param desiredResultType 调用者期望的结果类型
	 * 
	 * @return 评估结果
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	<T> T getValue(EvaluationContext context, Object rootObject, Class<T> desiredResultType)
			throws EvaluationException;

	/**
	 * 返回可以使用默认上下文传递给{@link #setValue}方法的最常规类型.
	 * 
	 * @return 可以在此上下文中设置的最常规类型的值
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	Class<?> getValueType() throws EvaluationException;

	/**
	 * 返回可以使用默认上下文传递给{@link #setValue(Object, Object)}方法的最常规类型.
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 可以在此上下文中设置的最常规类型的值
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	Class<?> getValueType(Object rootObject) throws EvaluationException;

	/**
	 * 返回可传递给给定上下文的{@link #setValue(EvaluationContext, Object)}方法的最常规类型.
	 * 
	 * @param context 评估表达式的上下文
	 * 
	 * @return 可以在此上下文中设置的最常规类型的值
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	Class<?> getValueType(EvaluationContext context) throws EvaluationException;

	/**
	 * 返回可传递给给定上下文的{@link #setValue(EvaluationContext, Object, Object)}方法的最常规类型.
	 * 提供的根对象将覆盖上下文中指定的任何对象.
	 * 
	 * @param context 评估表达式的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 可以在此上下文中设置的最常规类型的值
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	Class<?> getValueType(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * 返回可以使用默认上下文传递给{@link #setValue}方法的最常规类型.
	 * 
	 * @return 可以在此上下文中设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	TypeDescriptor getValueTypeDescriptor() throws EvaluationException;

	/**
	 * 返回可以使用默认上下文传递给{@link #setValue(Object, Object)}方法的最常规类型.
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 可以在此上下文中设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	TypeDescriptor getValueTypeDescriptor(Object rootObject) throws EvaluationException;

	/**
	 * 返回可传递给给定上下文的{@link #setValue(EvaluationContext, Object)}方法的最常规类型.
	 * 
	 * @param context 评估表达式的上下文
	 * 
	 * @return 可以在此上下文中设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context) throws EvaluationException;

	/**
	 * 返回可传递给给定上下文的{@link #setValue(EvaluationContext, Object, Object)}方法的最常规类型.
	 * 提供的根对象将覆盖上下文中指定的任何对象.
	 * 
	 * @param context 评估表达式的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return 可以在此上下文中设置的值的类型描述符
	 * @throws EvaluationException 如果确定类型时有问题
	 */
	TypeDescriptor getValueTypeDescriptor(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * 确定是否可以写入表达式, i.e. 可以调用setValue().
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return {@code true}如果表达式是可写的; 否则{@code false}
	 * @throws EvaluationException 如果确定它是否可写时有问题
	 */
	boolean isWritable(Object rootObject) throws EvaluationException;

	/**
	 * 确定是否可以写入表达式, i.e. 可以调用setValue().
	 * 
	 * @param context 应该检查表达式的上下文
	 * 
	 * @return {@code true} 如果表达式是可写的; 否则{@code false}
	 * @throws EvaluationException 如果确定它是否可写时有问题
	 */
	boolean isWritable(EvaluationContext context) throws EvaluationException;

	/**
	 * 确定是否可以写入表达式, i.e. 可以调用setValue().
	 * 提供的根对象将覆盖上下文中指定的任何对象.
	 * 
	 * @param context 应该检查表达式的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * 
	 * @return {@code true} 如果表达式是可写的; 否则{@code false}
	 * @throws EvaluationException 如果确定它是否可写时有问题
	 */
	boolean isWritable(EvaluationContext context, Object rootObject) throws EvaluationException;

	/**
	 * 在提供的上下文中将此表达式设置为提供的值.
	 * 
	 * @param rootObject 用于评估表达式的根对象
	 * @param value 新值
	 * 
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	void setValue(Object rootObject, Object value) throws EvaluationException;

	/**
	 * 在提供的上下文中将此表达式设置为提供的值.
	 * 
	 * @param context 设置表达式值的上下文
	 * @param value 新值
	 * 
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	void setValue(EvaluationContext context, Object value) throws EvaluationException;

	/**
	 * 在提供的上下文中将此表达式设置为提供的值.
	 * 提供的根对象将覆盖上下文中指定的任何对象.
	 * 
	 * @param context 设置表达式值的上下文
	 * @param rootObject 用于评估表达式的根对象
	 * @param value 新值
	 * 
	 * @throws EvaluationException 如果在评估过程中出现问题
	 */
	void setValue(EvaluationContext context, Object rootObject, Object value) throws EvaluationException;

}
