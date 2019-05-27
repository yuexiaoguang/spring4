package org.springframework.jca.cci.core.support;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.resource.cci.Record;
import javax.resource.cci.Streamable;

import org.springframework.util.FileCopyUtils;

/**
 * CCI Record implementation for a COMMAREA, holding a byte array.
 */
@SuppressWarnings("serial")
public class CommAreaRecord implements Record, Streamable {

	private byte[] bytes;

	private String recordName;

	private String recordShortDescription;


	/**
	 * Create a new CommAreaRecord.
	 */
	public CommAreaRecord() {
	}

	/**
	 * Create a new CommAreaRecord.
	 * @param bytes the bytes to fill the record with
	 */
	public CommAreaRecord(byte[] bytes) {
		this.bytes = bytes;
	}


	@Override
	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}

	@Override
	public String getRecordName() {
		return this.recordName;
	}

	@Override
	public void setRecordShortDescription(String recordShortDescription) {
		this.recordShortDescription = recordShortDescription;
	}

	@Override
	public String getRecordShortDescription() {
		return this.recordShortDescription;
	}


	@Override
	public void read(InputStream in) throws IOException {
		this.bytes = FileCopyUtils.copyToByteArray(in);
	}

	@Override
	public void write(OutputStream out) throws IOException {
		out.write(this.bytes);
		out.flush();
	}

	public byte[] toByteArray() {
		return this.bytes;
	}


	@Override
	public Object clone() {
		return new CommAreaRecord(this.bytes);
	}

}
