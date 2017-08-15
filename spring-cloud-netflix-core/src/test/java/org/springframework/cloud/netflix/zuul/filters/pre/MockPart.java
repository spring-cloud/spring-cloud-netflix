package org.springframework.cloud.netflix.zuul.filters.pre;

import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

class MockPart implements Part {
    private final String name;
    private final String contentType;
    private final String submittedFileName;
    private final Map<String, List<String>> headers;
    private final byte[] body;

    public MockPart(final String name, final String contentType, final String submittedFileName, final Map<String, List<String>> headers, final byte[] body) {
        this.name = name;
        this.contentType = contentType;
        this.submittedFileName = submittedFileName;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.body);
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getSubmittedFileName() {
        return this.submittedFileName;
    }

    @Override
    public long getSize() {
        return this.body != null ? this.body.length : 0;
    }

    @Override
    public void write(String fileName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHeader(String name) {
        if (this.headers == null) {
            return null;
        }

        final List<String> values = this.headers.get(name);

        if (values == null || values.size() == 0) {
            return null;
        }

        return values.get(0);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        if (this.headers == null) {
            return null;
        }

        return this.headers.get(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        if (this.headers == null) {
            return null;
        }

        return this.headers.keySet();
    }
}
