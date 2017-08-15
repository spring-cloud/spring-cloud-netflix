package org.springframework.cloud.netflix.zuul.filters.pre;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * @author Andre Dörnbrack
 */
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
    public synchronized void reset() throws IOException {
        input.reset();
    }
}
