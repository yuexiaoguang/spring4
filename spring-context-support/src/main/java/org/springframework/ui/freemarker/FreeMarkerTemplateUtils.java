package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 用于使用FreeMarker的实用程序类.
 * 提供使用模型处理FreeMarker模板的便捷方法.
 */
public abstract class FreeMarkerTemplateUtils {

	/**
	 * 使用给定模型处理指定的FreeMarker模板, 并将结果写入给定的Writer.
	 * <p>使用此方法为使用Spring的邮件支持发送的邮件准备文本时, 请考虑在MailPreparationException中包装IO/TemplateException.
	 * 
	 * @param model 模型对象, 通常是包含模型名称作为键, 并将模型对象作为值的Map
	 * 
	 * @return 结果
	 * @throws IOException 如果找不到模板或无法读取模板
	 * @throws freemarker.template.TemplateException 如果渲染失败
	 */
	public static String processTemplateIntoString(Template template, Object model)
			throws IOException, TemplateException {

		StringWriter result = new StringWriter();
		template.process(model, result);
		return result.toString();
	}

}
