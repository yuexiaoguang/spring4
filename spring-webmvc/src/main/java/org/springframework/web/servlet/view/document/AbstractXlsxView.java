package org.springframework.web.servlet.view.document;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Office 2007 XLSX格式的Excel文档视图的便捷超类 (由POI-OOXML支持).
 * 与Apache POI 3.5及更高版本兼容.
 *
 * <p>For working with the workbook in subclasses, see <a href="http://poi.apache.org">Apache's POI site</a>.
 */
public abstract class AbstractXlsxView extends AbstractXlsView {

	/**
	 * <p>Sets the content type of the view to
	 * {@code "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"}.
	 */
	public AbstractXlsxView() {
		setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}

	/**
	 * 此实现为XLSX格式创建{@link XSSFWorkbook}.
	 */
	@Override
	protected Workbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new XSSFWorkbook();
	}

}
