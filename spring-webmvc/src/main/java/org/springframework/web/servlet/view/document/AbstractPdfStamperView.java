package org.springframework.web.servlet.view.document;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * 用于使用AcroForm对现有文档进行操作的PDF视图的抽象超类.
 * 特定于应用程序的视图类将扩展此类以将PDF表单与模型数据合并.
 *
 * <p>此视图实现使用Bruno Lowagie的<a href="http://www.lowagie.com/iText">iText</a> API.
 * 已知可以使用原始的iText 2.1.7及其分支<a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>.
 * <b>强烈建议使用OpenPDF, 因为它是主动维护的, 并修复了不受信任的PDF内容的重要漏洞.</b>
 *
 * <p>Thanks to Bryant Larsen for the suggestion and the original prototype!
 */
public abstract class AbstractPdfStamperView extends AbstractUrlBasedView {

	public AbstractPdfStamperView(){
		setContentType("application/pdf");
	}


	@Override
	protected boolean generatesDownloadContent() {
		return true;
	}

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// IE workaround: write into byte array first.
		ByteArrayOutputStream baos = createTemporaryOutputStream();

		PdfReader reader = readPdfResource();
		PdfStamper stamper = new PdfStamper(reader, baos);
		mergePdfDocument(model, stamper, request, response);
		stamper.close();

		// Flush to HTTP response.
		writeToResponse(response, baos);
	}

	/**
	 * 将原始PDF资源读入iText PdfReader.
	 * <p>默认实现将指定的"url"属性解析为 ApplicationContext资源.
	 * 
	 * @return PdfReader实例
	 * @throws IOException 如果资源访问失败
	 */
	protected PdfReader readPdfResource() throws IOException {
		return new PdfReader(getApplicationContext().getResource(getUrl()).getInputStream());
	}

	/**
	 * 子类必须实现此方法以将PDF表单与给定的模型数据合并.
	 * <p>这是可以在AcroForm上设置值的地方. 在这个级别可以做的一个例子是:
	 * <pre class="code">
	 * // 从文档中获取表单
	 * AcroFields form = stamper.getAcroFields();
	 *
	 * // 在表单上设置一些值
	 * form.setField("field1", "value1");
	 * form.setField("field2", "Vvlue2");
	 *
	 * // 设置配置和文件名
	 * response.setHeader("Content-disposition", "attachment; FILENAME=someName.pdf");</pre>
	 * <p>请注意, 传入的HTTP响应应该用于设置cookie或其他 HTTP header.
	 * 在此方法返回后, 构建的PDF文档本身将自动写入响应.
	 * 
	 * @param model 模型Map
	 * @param stamper 包含AcroFields的PdfStamper实例.
	 * 也可以根据需要自定义此PdfStamper实例, e.g. 设置"formFlattening"属性.
	 * @param request 以防需要语言环境等. 不应该查看属性.
	 * @param response 以防需要设置cookie. 不应该写入它.
	 * 
	 * @throws Exception 文档构建期间发生的任何异常
     */
	protected abstract void mergePdfDocument(Map<String, Object> model, PdfStamper stamper,
			HttpServletRequest request, HttpServletResponse response) throws Exception;

}
