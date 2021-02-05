package eu.arrowhead.core.plantdescriptionengine.utils;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoReadable;
import se.arkalix.net.http.HttpHeaders;
import se.arkalix.net.http.HttpStatus;
import se.arkalix.net.http.HttpVersion;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.client.HttpClientResponse;
import se.arkalix.util.concurrent.FutureProgress;

/**
 * Mock HttpClientResponse implementation used for testing.
 */
public class MockClientResponse implements HttpClientResponse {

    private Object _body = null;
    private HttpStatus _status = HttpStatus.IM_A_TEAPOT;
	@Override
	public <R extends DtoReadable> FutureProgress<R> bodyAs(DtoEncoding encoding, Class<R> class_) {
        @SuppressWarnings("unchecked")
        R castBody = (R)_body;
        return new MockFutureProgress<R>(castBody);
	}
	@Override
	public FutureProgress<byte[]> bodyAsByteArray() {
		return null;
	}
	@Override
	public <R extends DtoReadable> FutureProgress<List<R>> bodyAsList(DtoEncoding encoding, Class<R> class_) {
		return null;
	}
	@Override
	public FutureProgress<? extends InputStream> bodyAsStream() {
		return null;
	}
	@Override
	public FutureProgress<String> bodyAsString() {
		return null;
	}
	@Override
	public FutureProgress<Path> bodyTo(Path path, boolean append) {
		return null;
	}
	@Override
	public HttpHeaders headers() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public HttpClientRequest request() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public HttpStatus status() {
		return _status;
	}
	@Override
	public HttpVersion version() {
		// TODO Auto-generated method stub
		return null;
    }

    public HttpClientResponse status(HttpStatus status) {
        _status = status;
        return this;
    }

    public HttpClientResponse body(Object data) {
        _body = data;
        return this;
    }
}