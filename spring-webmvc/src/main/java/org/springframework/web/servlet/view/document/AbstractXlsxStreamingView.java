package org.springframework.web.servlet.view.document;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * 使用POI的流式变体, Office 2007 XLSX格式的Excel文档视图的便捷超类.
 * 与Apache POI 3.9及更高版本兼容.
 *
 * <p>For working with the workbook in subclasses, see
 * <a href="http://poi.apache.org">Apache's POI site</a>.
 */
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

	/**
	 * 此实现创建了用于流式XLSX格式的{@link SXSSFWorkbook}.
	 */
	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new SXSSFWorkbook();
	}

	/**
	 * 完成渲染后, 此实现将处理{@link SXSSFWorkbook}.
	 */
	@Override
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		super.renderWorkbook(workbook, response);

		// Dispose of temporary files in case of streaming variant...
		((SXSSFWorkbook) workbook).dispose();
	}

}
