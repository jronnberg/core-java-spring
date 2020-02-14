package eu.arrowhead.core.translator.service.translator.spokes;

public interface BaseSpokeProducer extends BaseSpoke {

    public void close();

    public String getAddress();

}
