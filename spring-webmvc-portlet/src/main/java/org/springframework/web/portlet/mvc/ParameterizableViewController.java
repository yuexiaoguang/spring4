package org.springframework.web.portlet.mvc;

import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.web.portlet.ModelAndView;

/**
 * 总是返回命名视图的普通控制器.
 * 可以使用公开的配置属性配置视图.
 * 该控制器提供了直接向JSP等视图发送请求的替代方法.
 * 这里的优点是不会暴露给客户端具体的视图技术, 而只是暴露给控制器URL; 具体视图将由ViewResolver确定.
 *
 * <p><b><a name="workflow">Workflow
 * (<a href="AbstractController.html#workflow">和超类定义的那些</a>):</b>
 * <ol>
 * <li>控制器接收渲染请求</li>
 * <li>调用{@link #handleRenderRequestInternal handleRenderRequestInternal},
 * 它只返回视图, 由配置属性{@code viewName}命名.</li>
 * </ol>
 *
 * <p>此控制器不处理操作请求.
 *
 * <p><b><a name="config">暴露的配置属性</a>
 * (<a href="AbstractController.html#config">和超类定义的那些</a>):</b>
 * <table border="1">
 * <tr>
 * <td><b>name</b></td>
 * <td><b>default</b></td>
 * <td><b>description</b></td>
 * </tr>
 * <tr>
 * <td>viewName</td>
 * <td><i>null</i></td>
 * <td>viewResolver将用于转发的视图的名称 (如果未设置此属性, 则在初始化期间将抛出异常)</td>
 * </tr>
 * </table>
 */
public class ParameterizableViewController extends AbstractController {

	private String viewName;


	/**
	 * 设置要委托的视图的名称.
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

	/**
	 * 返回要委托的视图的名称.
	 */
	public String getViewName() {
		return this.viewName;
	}

	@Override
	protected void initApplicationContext() {
		if (this.viewName == null) {
			throw new IllegalArgumentException("Property 'viewName' is required");
		}
	}


	/**
	 * 返回具有指定视图名称的ModelAndView对象.
	 */
	@Override
	protected ModelAndView handleRenderRequestInternal(RenderRequest request, RenderResponse response)
			throws Exception {

		return new ModelAndView(getViewName());
	}

}
