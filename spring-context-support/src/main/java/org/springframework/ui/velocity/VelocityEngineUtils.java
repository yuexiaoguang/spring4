package org.springframework.ui.velocity;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.VelocityException;

/**
 * 用于使用VelocityEngine的实用程序类.
 * 提供将Velocity模板与模型合并的便捷方法.
 *
 * @deprecated as of Spring 4.3, in favor of FreeMarker
 */
@Deprecated
public abstract class VelocityEngineUtils {

	/**
	 * 将指定的Velocity模板与给定的模型合并, 并将结果写入给定的Writer.
	 * 
	 * @param velocityEngine 要使用的VelocityEngine
	 * @param templateLocation 模板的位置, 相对于Velocity的资源加载器路径
	 * @param model 模型名称作为键, 并将模型对象作为值的Map
	 * @param writer 要将结果写入的Writer
	 * 
	 * @throws VelocityException 如果找不到模板或渲染失败
	 * 
	 * @deprecated Use {@link #mergeTemplate(VelocityEngine, String, String, Map, Writer)}
	 * instead, following Velocity 1.6's corresponding deprecation in its own API.
	 */
	@Deprecated
	public static void mergeTemplate(
			VelocityEngine velocityEngine, String templateLocation, Map<String, Object> model, Writer writer)
			throws VelocityException {

		VelocityContext velocityContext = new VelocityContext(model);
		velocityEngine.mergeTemplate(templateLocation, velocityContext, writer);
	}

	/**
	 * 将指定的Velocity模板与给定的模型合并, 并将结果写入给定的Writer.
	 * 
	 * @param velocityEngine 要使用的VelocityEngine
	 * @param templateLocation 模板的位置, 相对于Velocity的资源加载器路径
	 * @param encoding 模板文件的编码
	 * @param model 模型名称作为键, 并将模型对象作为值的Map
	 * @param writer 要将结果写入的Writer
	 * 
	 * @throws VelocityException 如果找不到模板或渲染失败
	 */
	public static void mergeTemplate(
			VelocityEngine velocityEngine, String templateLocation, String encoding,
			Map<String, Object> model, Writer writer) throws VelocityException {

		VelocityContext velocityContext = new VelocityContext(model);
		velocityEngine.mergeTemplate(templateLocation, encoding, velocityContext, writer);
	}

	/**
	 * 将指定的Velocity模板与给定的模型合并为String.
	 * <p>使用此方法为使用Spring的邮件支持发送的邮件准备文本时,
	 * 请考虑在MailPreparationException中包装VelocityException.
	 * 
	 * @param velocityEngine 要使用的VelocityEngine
	 * @param templateLocation 模板的位置, 相对于Velocity的资源加载器路径
	 * @param model 模型名称作为键, 并将模型对象作为值的Map
	 * 
	 * @return 结果
	 * @throws VelocityException 如果找不到模板或渲染失败
	 * 
	 * @deprecated Use {@link #mergeTemplateIntoString(VelocityEngine, String, String, Map)}
	 * instead, following Velocity 1.6's corresponding deprecation in its own API.
	 */
	@Deprecated
	public static String mergeTemplateIntoString(VelocityEngine velocityEngine, String templateLocation,
			Map<String, Object> model) throws VelocityException {

		StringWriter result = new StringWriter();
		mergeTemplate(velocityEngine, templateLocation, model, result);
		return result.toString();
	}

	/**
	 * 将指定的Velocity模板与给定的模型合并为String.
	 * <p>使用此方法为使用Spring的邮件支持发送的邮件准备文本时,
	 * 请考虑在MailPreparationException中包装VelocityException.
	 *  
	 * @param velocityEngine 要使用的VelocityEngine
	 * @param templateLocation 模板的位置, 相对于Velocity的资源加载器路径
	 * @param encoding 模板文件的编码
	 * @param model 模型名称作为键, 并将模型对象作为值的Map
	 * 
	 * @return 结果
	 * @throws VelocityException 如果找不到模板或渲染失败
	 */
	public static String mergeTemplateIntoString(VelocityEngine velocityEngine, String templateLocation,
			String encoding, Map<String, Object> model) throws VelocityException {

		StringWriter result = new StringWriter();
		mergeTemplate(velocityEngine, templateLocation, encoding, model, result);
		return result.toString();
	}

}
