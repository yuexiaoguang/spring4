package org.springframework.validation;

import java.util.List;

import org.springframework.beans.PropertyAccessor;

/**
 * 存储和公开有关特定对象的数据绑定和验证错误的信息.
 *
 * <p>字段名称可以是目标对象的属性 (e.g. 绑定到客户对象时为"name"), 或者是子对象的嵌套字段 (e.g. "address.street").
 * 通过{@link #setNestedPath(String)}支持子树导航:
 * 例如, {@code AddressValidator}验证"address", 不知道这是客户的子对象.
 *
 * <p>Note: {@code Errors}对象是单线程的.
 */
public interface Errors {

	/**
	 * 嵌套路径中路径元素之间的分隔符, 例如"customer.name"或"customer.address.street".
	 * <p>"." = 等同于beans包中的
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR nested property separator}.
	 */
	String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;


	/**
	 * 返回绑定根对象的名称.
	 */
	String getObjectName();

	/**
	 * 允许更改上下文, 以便标准验证器可以验证子树.
	 * 拒绝调用将给定路径添加到字段名称之前.
	 * <p>例如, 地址验证器可以验证客户对象的子对象"address".
	 * 
	 * @param nestedPath 此对象中的嵌套路径, e.g. "address" (默认为"", {@code null}也可以接受).
	 * 可以用点结束: "address"和"address."都有效.
	 */
	void setNestedPath(String nestedPath);

	/**
	 * 返回此{@link Errors}对象的当前嵌套路径.
	 * <p>返回带点的嵌套路径, i.e. "address.", 便于构建连接路径.
	 * 默认为空字符串.
	 */
	String getNestedPath();

	/**
	 * 将给定的子路径推送到嵌套的路径堆栈.
	 * <p>{@link #popNestedPath()}调用将在相应的{@code pushNestedPath(String)}调用之前重置原始嵌套路径.
	 * <p>使用嵌套路径堆栈允许为子对象设置临时嵌套路径, 而不必担心临时路径持有者.
	 * <p>例如: 当前路径"spouse.", pushNestedPath("child") -> 结果路径"spouse.child."; popNestedPath() -> "spouse." again.
	 * 
	 * @param subPath 推入嵌套路径堆栈的子路径
	 */
	void pushNestedPath(String subPath);

	/**
	 * 从嵌套路径堆栈弹出以前的嵌套路径.
	 * 
	 * @throws IllegalStateException 如果堆栈上没有以前的嵌套路径
	 */
	void popNestedPath() throws IllegalStateException;

	/**
	 * 使用给定的错误描述为整个目标对象注册全局错误.
	 * 
	 * @param errorCode 错误代码, 可解释为消息Key
	 */
	void reject(String errorCode);

	/**
	 * 使用给定的错误描述为整个目标对象注册全局错误.
	 * 
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param defaultMessage 后备默认消息
	 */
	void reject(String errorCode, String defaultMessage);

	/**
	 * 使用给定的错误描述为整个目标对象注册全局错误..
	 * 
	 * @param errorCode 错误代码, 可解释为消息Key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 * @param defaultMessage 后备默认消息
	 */
	void reject(String errorCode, Object[] errorArgs, String defaultMessage);

	/**
	 * 使用给定的错误描述为当前对象的指定字段注册字段错误 (遵守当前嵌套路径).
	 * <p>字段名称可以是{@code null}或空字符串, 以指示当前对象本身而不是其字段.
	 * 如果当前对象是顶层对象, 这可能导致嵌套对象图中的相应字段错误或全局错误.
	 * 
	 * @param field 字段名称 (可能是{@code null}或空字符串)
	 * @param errorCode 错误代码, 可解释为消息key
	 */
	void rejectValue(String field, String errorCode);

	/**
	 * 使用给定的错误描述为当前对象的指定字段注册字段错误 (遵守当前嵌套路径).
	 * <p>字段名称可以是{@code null}或空字符串, 以指示当前对象本身而不是其字段.
	 * 如果当前对象是顶层对象, 这可能导致嵌套对象图中的相应字段错误或全局错误.
	 * 
	 * @param field 字段名称 (可能是{@code null}或空字符串)
	 * @param errorCode 错误代码, 可解释为消息key
	 * @param defaultMessage 后备默认消息
	 */
	void rejectValue(String field, String errorCode, String defaultMessage);

	/**
	 * 使用给定的错误描述为当前对象的指定字段注册字段错误 (遵守当前嵌套路径).
	 * <p>字段名称可以是{@code null}或空字符串, 以指示当前对象本身而不是其字段.
	 * 如果当前对象是顶层对象, 这可能导致嵌套对象图中的相应字段错误或全局错误.
	 * 
	 * @param field 字段名称 (可能是{@code null}或空字符串)
	 * @param errorCode 错误代码, 可解释为消息key
	 * @param errorArgs 错误参数, 用于通过MessageFormat进行参数绑定 (can be {@code null})
	 * @param defaultMessage 后备默认消息
	 */
	void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage);

	/**
	 * 将给定{@code Errors}实例中的所有错误添加到此{@code Errors}实例.
	 * <p>这是一种方便的方法, 可以避免重复{@code reject(..)}调用, 将{@code Errors}实例合并到另一个{@code Errors}实例中.
	 * <p>请注意, 传入的{@code Errors}实例应该引用相同的目标对象, 或者至少包含适用于此{@code Errors}实例的目标对象的兼容错误.
	 * 
	 * @param errors 要合并的{@code Errors}实例
	 */
	void addAllErrors(Errors errors);

	/**
	 * 返回是否有错误.
	 */
	boolean hasErrors();

	/**
	 * 返回错误总数.
	 */
	int getErrorCount();

	/**
	 * 获取全局和字段错误.
	 * 
	 * @return {@link ObjectError}实例列表
	 */
	List<ObjectError> getAllErrors();

	/**
	 * 是否有全局错误?
	 * 
	 * @return {@code true}如果有
	 */
	boolean hasGlobalErrors();

	/**
	 * 返回全局错误的数量.
	 * 
	 * @return 全局错误的数量
	 */
	int getGlobalErrorCount();

	/**
	 * 获取所有全局错误.
	 * 
	 * @return {@link ObjectError}实例列表
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * 获取<i>第一个</i>全局错误.
	 * 
	 * @return 全局错误, 或{@code null}
	 */
	ObjectError getGlobalError();

	/**
	 * 是否有字段错误?
	 * 
	 * @return {@code true}如果有任何与字段相关的错误
	 */
	boolean hasFieldErrors();

	/**
	 * 返回与字段关联的错误数量.
	 * 
	 * @return 与字段关联的错误数量
	 */
	int getFieldErrorCount();

	/**
	 * 获取与字段关联的所有错误.
	 * 
	 * @return {@link FieldError}实例列表
	 */
	List<FieldError> getFieldErrors();

	/**
	 * 获取与字段关联的<i>第一个</i>错误.
	 * 
	 * @return 特定于字段的错误, 或{@code null}
	 */
	FieldError getFieldError();

	/**
	 * 是否存在与给定字段相关的错误?
	 * 
	 * @param field 字段名称
	 * 
	 * @return {@code true}如果有
	 */
	boolean hasFieldErrors(String field);

	/**
	 * 返回与给定字段关联的错误数量.
	 * 
	 * @param field 字段名称
	 * 
	 * @return 与给定字段关联的错误数量
	 */
	int getFieldErrorCount(String field);

	/**
	 * 获取与给定字段关联的所有错误.
	 * <p>实现不仅应该支持像"name"这样的完整字段名称, 还应该支持"na*" 或 "address.*"等模式匹配.
	 * 
	 * @param field 字段名称
	 * 
	 * @return {@link FieldError}实例列表
	 */
	List<FieldError> getFieldErrors(String field);

	/**
	 * 获取与给定字段关联的第一个错误.
	 * 
	 * @param field 字段名称
	 * 
	 * @return 特定于字段的错误, 或{@code null}
	 */
	FieldError getFieldError(String field);

	/**
	 * 返回给定字段的当前值, 当前bean属性值或上次绑定时拒绝的更新.
	 * <p>即使存在类型不匹配, 也可以方便地访问用户指定的字段值.
	 * 
	 * @param field 字段名称
	 * 
	 * @return 给定字段的当前值
	 */
	Object getFieldValue(String field);

	/**
	 * 返回给定字段的类型.
	 * <p>即使字段值为{@code null}, 实现也应该能够确定类型, 例如来自某些关联的描述符.
	 * 
	 * @param field 字段名称
	 * 
	 * @return 字段的类型, 或{@code null}
	 */
	Class<?> getFieldType(String field);

}
