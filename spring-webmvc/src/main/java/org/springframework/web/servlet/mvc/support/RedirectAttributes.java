package org.springframework.web.servlet.mvc.support;

import java.util.Collection;
import java.util.Map;

import org.springframework.ui.Model;
import org.springframework.web.servlet.FlashMap;

/**
 * {@link Model}接口的细化, 控制器可用于为重定向场景选择属性.
 * 由于添加重定向属性的意图非常明确 --  i.e. 用于重定向URL, 属性值可以格式化为字符串并以这种方式存储,
 * 以使它们有资格附加到查询字符串, 或在{@code org.springframework.web.servlet.view.RedirectView}中扩展为URI变量.
 *
 * <p>此接口还提供了添加Flash属性的方法. 有关Flash属性的一般概述, 请参阅{@link FlashMap}.
 * 可以使用{@link RedirectAttributes}存储Flash属性, 它们将自动传播到当前请求的"output" FlashMap.
 *
 * <p>{@code @Controller}中的示例用法:
 * <pre class="code">
 * &#064;RequestMapping(value = "/accounts", method = RequestMethod.POST)
 * public String handle(Account account, BindingResult result, RedirectAttributes redirectAttrs) {
 *   if (result.hasErrors()) {
 *     return "accounts/new";
 *   }
 *   // Save account ...
 *   redirectAttrs.addAttribute("id", account.getId()).addFlashAttribute("message", "Account created!");
 *   return "redirect:/accounts/{id}";
 * }
 * </pre>
 *
 * <p>调用方法时, RedirectAttributes模型为空, 除非方法返回重定向视图名称或RedirectView, 否则永远不会使用.
 *
 * <p>重定向后, Flash属性会自动添加到为目标URL提供服务的控制器模型中.
 */
public interface RedirectAttributes extends Model {

	@Override
	RedirectAttributes addAttribute(String attributeName, Object attributeValue);

	@Override
	RedirectAttributes addAttribute(Object attributeValue);

	@Override
	RedirectAttributes addAllAttributes(Collection<?> attributeValues);

	@Override
	RedirectAttributes mergeAttributes(Map<String, ?> attributes);

	/**
	 * 添加给定的flash属性.
	 * 
	 * @param attributeName 属性名称; never {@code null}
	 * @param attributeValue 属性值; may be {@code null}
	 */
	RedirectAttributes addFlashAttribute(String attributeName, Object attributeValue);

	/**
	 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}添加给定的flash存储.
	 * 
	 * @param attributeValue flash属性值; never {@code null}
	 */
	RedirectAttributes addFlashAttribute(Object attributeValue);

	/**
	 * 返回flash存储的候选属性或空Map.
	 */
	Map<String, ?> getFlashAttributes();
}
