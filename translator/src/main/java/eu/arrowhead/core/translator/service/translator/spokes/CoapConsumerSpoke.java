package eu.arrowhead.core.translator.service.translator.spokes;

import java.net.UnknownHostException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class CoapConsumerSpoke implements BaseSpokeConsumer {

    BaseSpoke nextSpoke;
    String serviceAddress = ""; //"coap://127.0.0.1:5692/";
    public int activity = 0;

    public CoapConsumerSpoke(String serviceAddress) throws UnknownHostException {
        if (serviceAddress.startsWith("coap")) {
            this.serviceAddress = serviceAddress;
        } else {
            this.serviceAddress = "coap://" + serviceAddress;
        }

        CoapClient pingClient = new CoapClient(serviceAddress);
        pingClient.ping();

    }

    @Override
    public void close() {
    }

    @Override
    public void in(BaseContext context) {
        //if the context has no error then
        //start a coap client worker
        new Thread(new Worker(context), "name").start();
    }

    @Override
    public void setNextSpoke(Object nextSpoke) {
        this.nextSpoke = (BaseSpoke) nextSpoke;
    }

    @Override
    public int getLastActivity() {
        return activity;
    }

    @Override
    public void clearActivity() {
        activity = 0;
    }

    class Worker implements Runnable {

        BaseContext context = null;

        public Worker(BaseContext paramContext) {
            this.context = paramContext;
        }

        @Override
        public void run() {
            if (serviceAddress.endsWith("/") && context.getPath().startsWith("/")) {
                context.setPath(context.getPath().substring(1));
            }

            CoapClient client = new CoapClient(serviceAddress + context.getPath());

            CoapResponse response = null;
            long lStartTime = System.nanoTime();
            System.out.println(lStartTime + ": CoAP sending to: " + client.getURI());

            
            
            switch(context.getMethod()) {
                case GET:
                    response = client.get();
                    break;
                case POST:
                    response = client.post(context.getContent(), MediaTypeRegistry.parse(context.getContentType()));
                    break;
                case PUT:
                    response = client.put(context.getContent(), MediaTypeRegistry.parse(context.getContentType()));
                    break;
                case DELETE:
                    response = client.delete();
                    break;
                default:
                    break;
            }

            lStartTime = System.nanoTime();
            System.out.println(lStartTime + ": CoAP response recieved");

            if (response != null) {
                context.setContent(response.getResponseText());
            } else {
                //TODO: need to signal an error to the next spoke
            }
            activity++;
            nextSpoke.in(context);
        }
    }
}
