package org.springframework.web.servlet.view.document;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Convenient superclass for Excel document views in the Office 2007 XLSX format,
 * using POI's streaming variant. Compatible with Apache POI 3.9 and higher.
 *
 * <p>For working with the workbook in subclasses, see
 * <a href="http://poi.apache.org">Apache's POI site</a>.
 */
public abstract class AbstractXlsxStreamingView extends AbstractXlsxView {

	/**
	 * This implementation creates a {@link SXSSFWorkbook} for streaming the XLSX format.
	 */
	@Override
	protected SXSSFWorkbook createWorkbook(Map<String, Object> model, HttpServletRequest request) {
		return new SXSSFWorkbook();
	}

	/**
	 * This implementation disposes of the {@link SXSSFWorkbook} when done with rendering.
	 */
	@Override
	protected void renderWorkbook(Workbook workbook, HttpServletResponse response) throws IOException {
		super.renderWorkbook(workbook, response);

		// Dispose of temporary files in case of streaming variant...
		((SXSSFWorkbook) workbook).dispose();
	}

}
