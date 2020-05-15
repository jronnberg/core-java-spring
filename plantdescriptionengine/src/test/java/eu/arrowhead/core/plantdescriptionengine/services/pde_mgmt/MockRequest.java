package eu.arrowhead.core.plantdescriptionengine.services.pde_mgmt;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.arkalix.description.ConsumerDescription;
import se.arkalix.dto.DtoEncoding;
import se.arkalix.dto.DtoReadable;
import se.arkalix.net.http.HttpHeaders;
import se.arkalix.net.http.HttpMethod;
import se.arkalix.net.http.HttpVersion;
import se.arkalix.net.http.service.HttpServiceRequest;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.FutureProgress;

class MockRequest implements HttpServiceRequest {


    public static class Builder {
        List<String> pathParameters = new ArrayList<String>();

        Builder pathParameters(List<String> pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        public MockRequest build() {
            return new MockRequest(this);
        }
    }

    private List<String> _pathParameters = List.of("0", "1", "2"); // TODO: Set in constructor

    public MockRequest() {}

    public MockRequest(Builder builder) {
        _pathParameters = builder.pathParameters;
    }

    @Override
    public <R extends DtoReadable> FutureProgress<R> bodyAs(DtoEncoding encoding, Class<R> class_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FutureProgress<byte[]> bodyAsByteArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends DtoReadable> FutureProgress<List<R>> bodyAsList(DtoEncoding encoding, Class<R> class_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FutureProgress<? extends InputStream> bodyAsStream() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FutureProgress<String> bodyAsString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FutureProgress<Path> bodyTo(Path path, boolean append) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends DtoReadable> FutureProgress<R> bodyAs(Class<R> class_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <R extends DtoReadable> FutureProgress<List<R>> bodyAsList(Class<R> class_) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpHeaders headers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpMethod method() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String path() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> pathParameters() {
        return _pathParameters;
    }

    @Override
    public Map<String, List<String>> queryParameters() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConsumerDescription consumer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public HttpVersion version() {
        throw new UnsupportedOperationException();
    }

}