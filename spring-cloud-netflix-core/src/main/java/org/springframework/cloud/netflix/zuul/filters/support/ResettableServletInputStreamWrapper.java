package org.springframework.cloud.netflix.zuul.filters.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;

public class ResettableServletInputStreamWrapper extends ServletInputStream {

	private final ByteArrayInputStream input;

	public ResettableServletInputStreamWrapper(byte[] data) {
		this.input = new ByteArrayInputStream(data);
	}

	@Override
	public boolean isFinished() {
		return false;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setReadListener(ReadListener listener) {
	}

	@Override
	public int read() throws IOException {
		return input.read();
	}

	@Override
	public void reset() throws IOException {
		synchronized (this) {
			input.reset();
		}
	}
}
