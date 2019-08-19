package org.springframework.web.servlet.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.web.servlet.HandlerMapping}接口的实现,
 * 从URL到名称以斜杠("/")开头的bean的映射, 类似于Struts将URL映射到操作名称的方式.
 *
 * <p>这是{@link org.springframework.web.servlet.DispatcherServlet}使用的默认实现,
 * 以及{@link org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping}.
 * 或者, {@link SimpleUrlHandlerMapping}允许以声明方式自定义处理器映射.
 *
 * <p>映射是从URL到bean名称. 因此, 传入的URL "/foo"将映射到名为"/foo"的处理器,
 * 或者在多个映射到单个处理器的情况下映射到"/foo /foo2".
 * Note: 在XML定义中, 需要在bean定义中使用别名 name="/foo", 因为XML id可能不包含斜杠.
 *
 * <p>支持直接匹配 (给定的"/test" -> 注册的"/test")和"*"匹配 (给定的"/test" -> 注册的"/t*").
 * 请注意, 默认情况下是在当前servlet映射中映射;
 * 有关详细信息, 请参阅{@link #setAlwaysUseFullPath "alwaysUseFullPath"}属性.
 * 有关模式选项的详细信息, 请参阅{@link org.springframework.util.AntPathMatcher} javadoc.
 */
public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

	/**
	 * 检查URL的给定bean的名称和别名, 以"/"开头.
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		List<String> urls = new ArrayList<String>();
		if (beanName.startsWith("/")) {
			urls.add(beanName);
		}
		String[] aliases = getApplicationContext().getAliases(beanName);
		for (String alias : aliases) {
			if (alias.startsWith("/")) {
				urls.add(alias);
			}
		}
		return StringUtils.toStringArray(urls);
	}

}
