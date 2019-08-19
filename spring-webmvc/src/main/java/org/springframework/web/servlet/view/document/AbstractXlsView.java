package org.springframework.web.servlet.view.document;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;

import org.springframework.web.servlet.view.AbstractView;

/**
 * 传统XLS格式的Excel文档视图的便捷超类.
 * 与Apache POI 3.5及更高版本兼容.
 *
 * <p>For working with the workbook in the subclass, see
 * <a href="http://poi.apache.org">Apache's POI site</a>
 */
public abstract class AbstractXlsView extends AbstractView {

	/**
	 * 将视图的内容类型设置为"application/vnd.ms-excel".
	 */
	public AbstractXlsView() {
		setContentType("application/vnd.ms-excel");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	/**
	 * 给定指定的模型, 呈现Excel视图.
	 */
	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// Create a fresh workbook instance for this render step.
		Workbook workbook = createWorkbook(model, request);

		// Delegate to application-provided document code.
		buildExcelDocument(model, workbook, request, response);

		// Set the content type.
		response.setContentType(getContentType());

		// Flush byte array to servlet output stream.
		renderWorkbook(workbook, response);
	}


	/**
	 * 用于创建POI {@link Workbook}实例的模板方法.
	 * <p>默认实现创建了一个传统的{@link HSSFWorkbook}.
	 * 对于基于OOXML的变体, Spring提供的子类是重写的; 自定义子类可以覆盖它以从文件中读取工作簿.
	 * 
	 * @param model 模型Map
	 * @param request 当前的HTTP请求 (用于考虑URL或header)
	 * 
	 * @return 新的{@link Workbook}实例
	 */
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new HSSFWorkbook();
	}

	/**
	 * 实际的渲染步骤: 获取POI {@link Workbook}并将其渲染到给定的响应.
	 * 
	 * @param workbook 要呈现的POI工作簿
	 * @param response 当前的HTTP响应
	 * 
	 * @throws IOException
	 */
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		workbook.write(out);

		// Closeable only implemented as of POI 3.10
		if (workbook instanceof Closeable) {
			((Closeable) workbook).close();
		}
	}

	/**
	 * 在给定模型的情况下, 应用程序提供的子类必须实现此方法来填充Excel工作簿文档.
	 * 
	 * @param model 模型 Map
	 * @param workbook 要填充的Excel工作簿
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 * @param response 以防需要设置cookie. 不应该写入它.
	 */
	protected abstract void buildExcelDocument(
			Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
