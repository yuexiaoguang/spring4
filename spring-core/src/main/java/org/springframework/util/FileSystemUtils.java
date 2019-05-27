package org.springframework.util;

import java.io.File;
import java.io.IOException;

/**
 * 用于处理文件系统的实用方法.
 */
public abstract class FileSystemUtils {

	/**
	 * 删除提供的{@link File} - 对于目录, 也以递归方式删除任何嵌套目录或文件.
	 * 
	 * @param root 要删除的根{@code File}
	 * 
	 * @return {@code true}如果{@code File}被删除, 否则{@code false}
	 */
	public static boolean deleteRecursively(File root) {
		if (root != null && root.exists()) {
			if (root.isDirectory()) {
				File[] children = root.listFiles();
				if (children != null) {
					for (File child : children) {
						deleteRecursively(child);
					}
				}
			}
			return root.delete();
		}
		return false;
	}

	/**
	 * 递归地将{@code src} 文件/目录的内容复制到{@code dest}文件/目录.
	 * 
	 * @param src 源目录
	 * @param dest 目标目录
	 * 
	 * @throws IOException 发生I/O错误
	 */
	public static void copyRecursively(File src, File dest) throws IOException {
		Assert.isTrue(src != null && (src.isDirectory() || src.isFile()),
				"Source File must denote a directory or file");
		Assert.notNull(dest, "Destination File must not be null");
		doCopyRecursively(src, dest);
	}

	/**
	 * 将{@code src} 文件/目录的内容复制到{@code dest}文件/目录.
	 * 
	 * @param src 源目录
	 * @param dest 目标目录
	 * 
	 * @throws IOException 发生I/O错误
	 */
	private static void doCopyRecursively(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdir();
			File[] entries = src.listFiles();
			if (entries == null) {
				throw new IOException("Could not list files in directory: " + src);
			}
			for (File entry : entries) {
				doCopyRecursively(entry, new File(dest, entry.getName()));
			}
		}
		else if (src.isFile()) {
			try {
				dest.createNewFile();
			}
			catch (IOException ex) {
				throw new IOException("Failed to create file: " + dest, ex);
			}
			FileCopyUtils.copy(src, dest);
		}
		else {
			// 特殊文件句柄: 既不是文件也不是目录.
			// Simply skip it when contained in nested directory...
		}
	}

}
