package org.springframework.web.multipart.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Apache Commons FileUpload的{@link MultipartFile}实现.
 */
@SuppressWarnings("serial")
public class CommonsMultipartFile implements MultipartFile, Serializable {

	protected static final Log logger = LogFactory.getLog(CommonsMultipartFile.class);

	private final FileItem fileItem;

	private final long size;

	private boolean preserveFilename = false;


	/**
	 * @param fileItem 要包装的FileItem
	 */
	public CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
		this.size = this.fileItem.getSize();
	}


	/**
	 * 返回底层{@code org.apache.commons.fileupload.FileItem}实例. 几乎没有必要访问它.
	 */
	public final FileItem getFileItem() {
		return this.fileItem;
	}

	/**
	 * 设置是否保留客户端发送的文件名, 而不是删除{@link CommonsMultipartFile#getOriginalFilename()}中的路径信息.
	 * <p>默认为"false", 剥离可能在实际文件名前加的路径信息, e.g. 从Opera上传的文件.
	 * 将其切换为"true"以保留客户端指定的文件名, 包括潜在的路径分隔符.
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public String getOriginalFilename() {
		String filename = this.fileItem.getName();
		if (filename == null) {
			// Should never happen.
			return "";
		}
		if (this.preserveFilename) {
			// 不要试图剥离路径...
			return filename;
		}

		// Check for Unix-style path
		int unixSep = filename.lastIndexOf('/');
		// Check for Windows-style path
		int winSep = filename.lastIndexOf('\\');
		// Cut off at latest possible point
		int pos = (winSep > unixSep ? winSep : unixSep);
		if (pos != -1)  {
			// Any sort of path separator found...
			return filename.substring(pos + 1);
		}
		else {
			// A plain name
			return filename;
		}
	}

	@Override
	public String getContentType() {
		return this.fileItem.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return (this.size == 0);
	}

	@Override
	public long getSize() {
		return this.size;
	}

	@Override
	public byte[] getBytes() {
		if (!isAvailable()) {
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		byte[] bytes = this.fileItem.get();
		return (bytes != null ? bytes : new byte[0]);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!isAvailable()) {
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		InputStream inputStream = this.fileItem.getInputStream();
		return (inputStream != null ? inputStream : StreamUtils.emptyInput());
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (!isAvailable()) {
			throw new IllegalStateException("File has already been moved - cannot be transferred again");
		}

		if (dest.exists() && !dest.delete()) {
			throw new IOException(
					"Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
		}

		try {
			this.fileItem.write(dest);
			if (logger.isDebugEnabled()) {
				String action = "transferred";
				if (!this.fileItem.isInMemory()) {
					action = (isAvailable() ? "copied" : "moved");
				}
				logger.debug("Multipart file '" + getName() + "' with original filename [" +
						getOriginalFilename() + "], stored " + getStorageDescription() + ": " +
						action + " to [" + dest.getAbsolutePath() + "]");
			}
		}
		catch (FileUploadException ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
		catch (IllegalStateException ex) {
			// 从FileItem直接传递
			throw ex;
		}
		catch (IOException ex) {
			// From I/O operations within FileItem.write
			throw ex;
		}
		catch (Exception ex) {
			throw new IOException("File transfer failed", ex);
		}
	}

	/**
	 * 确定multipart内容是否仍然可用.
	 * 如果移动了临时文件, 则内容不再可用.
	 */
	protected boolean isAvailable() {
		// 如果在内存中, 它是可用的.
		if (this.fileItem.isInMemory()) {
			return true;
		}
		// 检查临时文件的实际存在.
		if (this.fileItem instanceof DiskFileItem) {
			return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
		}
		// 检查当前文件大小是否与原始文件大小不同.
		return (this.fileItem.getSize() == this.size);
	}

	/**
	 * 返回multipart内容的存储位置的描述.
	 * 尝试尽可能具体: 在临时文件的情况下提到文件位置.
	 */
	public String getStorageDescription() {
		if (this.fileItem.isInMemory()) {
			return "in memory";
		}
		else if (this.fileItem instanceof DiskFileItem) {
			return "at [" + ((DiskFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
		}
		else {
			return "on disk";
		}
	}

}
