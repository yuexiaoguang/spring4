package org.springframework.web.context.support;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.support.LiveBeansView;

/**
 * {@link LiveBeansView}的MBean公开的Servlet变体.
 *
 * <p>在当前Web应用程序中生成的所有ApplicationContexts中为当前Bean及其依赖项生成JSON快照.
 */
@SuppressWarnings("serial")
public class LiveBeansViewServlet extends HttpServlet {

	private LiveBeansView liveBeansView;


	@Override
	public void init() throws ServletException {
		this.liveBeansView = buildLiveBeansView();
	}

	protected LiveBeansView buildLiveBeansView() {
		return new ServletContextLiveBeansView(getServletContext());
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String content = this.liveBeansView.getSnapshotAsJson();
		response.setContentType("application/json");
		response.setContentLength(content.length());
		response.getWriter().write(content);
	}

}
