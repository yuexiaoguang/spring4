package org.springframework.validation;

import java.beans.PropertyEditor;
import java.util.Map;

import org.springframework.beans.PropertyEditorRegistry;

/**
 * 表示绑定结果的通用接口.
 * 扩展{@link Errors}接口以获取错误注册功能, 允许应用{@link Validator}, 并添加特定于绑定的分析和模型构建.
 *
 * <p>作为{@link DataBinder}的结果持有者, 通过{@link DataBinder#getBindingResult()}方法获得.
 * BindingResult实现也可以直接使用, 例如在其上调用{@link Validator}(e.g. 作为单元测试的一部分).
 */
public interface BindingResult extends Errors {

	/**
	 * 模型中BindingResult实例名称的前缀, 后跟对象名称.
	 */
	String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";


	/**
	 * 返回包装的目标对象, 可以是bean, 具有public字段的对象, Map - 取决于具体的绑定策略.
	 */
	Object getTarget();

	/**
	 * 返回获取状态的模型Map, 将BindingResult实例公开为 '{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName',
	 * 将对象本身公开为'objectName'.
	 * <p>请注意, 每次调用此方法时都会构建Map.
	 * 将内容添加到Map, 然后重新调用此方法将不起作用.
	 * <p>此方法返回的模型Map中的属性通常包含在{@link org.springframework.web.servlet.ModelAndView}中,
	 * 用于在JSP中使用Spring的{@code bind}标记的表单视图, 该标记需要访问BindingResult实例.
	 * 在渲染表单视图时, Spring的预构建表单控制器将执行此操作.
	 * 在自己构建ModelAndView实例时, 需要包含此方法返回的模型Map中的属性.
	 */
	Map<String, Object> getModel();

	/**
	 * 提取给定字段的原始字段值.
	 * 通常用于比较目的.
	 * 
	 * @param field 要检查的字段
	 * 
	 * @return 原始形式的字段的当前值, 或{@code null}
	 */
	Object getRawFieldValue(String field);

	/**
	 * 查找给定类型和属性的自定义属性编辑器.
	 * 
	 * @param field 属性的路径(名称或嵌套路径), 或{@code null} 如果查找给定类型的所有属性的编辑器
	 * @param valueType 属性的类型(如果给出了属性, 则可以是{@code null}, 但在任何情况下都应指定用于一致性检查)
	 * 
	 * @return 注册的编辑器, 或{@code null}
	 */
	PropertyEditor findEditor(String field, Class<?> valueType);

	/**
	 * 返回底层PropertyEditorRegistry.
	 * 
	 * @return PropertyEditorRegistry, 或{@code null}如果没有可用于此BindingResult的
	 */
	PropertyEditorRegistry getPropertyEditorRegistry();

	/**
	 * 将自定义{@link ObjectError}或{@link FieldError}添加到错误列表中.
	 * <p>旨在被{@link BindingErrorProcessor}等合作策略使用.
	 */
	void addError(ObjectError error);

	/**
	 * 将给定的错误代码解析为消息代码.
	 * <p>使用适当的参数调用已配置的{@link MessageCodesResolver}.
	 * 
	 * @param errorCode 要解析为消息代码的错误代码
	 * 
	 * @return 要解析的消息代码
	 */
	String[] resolveMessageCodes(String errorCode);

	/**
	 * 将给定的错误代码解析为给定字段的消息代码.
	 * <p>使用适当的参数调用已配置的{@link MessageCodesResolver}.
	 * 
	 * @param errorCode 要解析为消息代码的错误代码
	 * @param field 用于解析消息代码的字段
	 * 
	 * @return 已解析的消息代码
	 */
	String[] resolveMessageCodes(String errorCode, String field);

	/**
	 * 将指定的不允许字段标记为已抑制.
	 * <p>数据绑定器会为检测到的每个字段值调用此方法以定位不允许的字段.
	 */
	void recordSuppressedField(String field);

	/**
	 * 返回绑定过程中被抑制的字段列表.
	 * <p>可用于确定是否有任何字段值定位为不允许的字段.
	 */
	String[] getSuppressedFields();

}
