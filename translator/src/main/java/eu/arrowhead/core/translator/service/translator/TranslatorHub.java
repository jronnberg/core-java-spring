package eu.arrowhead.core.translator.service.translator;

import eu.arrowhead.core.translator.service.translator.common.TranslatorDef.EndPoint;
import eu.arrowhead.core.translator.service.translator.spokes.BaseSpokeConsumer;
import eu.arrowhead.core.translator.service.translator.spokes.CoapConsumerSpoke;
import eu.arrowhead.core.translator.service.translator.spokes.CoapProducerSpoke;
import eu.arrowhead.core.translator.service.translator.spokes.HttpConsumerSpoke;
import eu.arrowhead.core.translator.service.translator.spokes.HttpProducerSpoke;
import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.arrowhead.core.translator.service.translator.spokes.BaseSpokeProducer;

public class TranslatorHub {
    private final Logger logger = LogManager.getLogger(TranslatorHub.class);
    
    private final int id;
    private final EndPoint pSpoke_Consumer;
    private final EndPoint cSpoke_Provider;
    private final BaseSpokeProducer pSpoke;
    private final BaseSpokeConsumer cSpoke;
    private final String hubIp = "0.0.0.0";
    private final int hubPort;

    public boolean noactivity = false;
    
    public TranslatorHub(int id, EndPoint pSpoke_Consumer, EndPoint cSpoke_Provider) throws Exception {
        logger.info("NEW HUB");
        this.id = id;
        this.pSpoke_Consumer = pSpoke_Consumer;
        this.cSpoke_Provider = cSpoke_Provider;
                
        switch(pSpoke_Consumer.getProtocol()) {
            case coap:
                pSpoke = new CoapProducerSpoke(hubIp);
                hubPort = new URI(pSpoke.getAddress()).getPort();
                break;
            case http:
                pSpoke = new HttpProducerSpoke(hubIp, "/*");
                hubPort = new URI(pSpoke.getAddress()).getPort();
                break;
            default:
                throw new Exception("Unknown protocol "+ pSpoke_Consumer.getProtocol());
        }
        
        switch(cSpoke_Provider.getProtocol()) {
            case coap:
                cSpoke = new CoapConsumerSpoke(cSpoke_Provider.getHostIpAddress());
                break;
            case http:
                cSpoke = new HttpConsumerSpoke(cSpoke_Provider.getHostIpAddress());
                break;
            default:
                throw new Exception("Unknown protocol "+ pSpoke_Consumer.getProtocol());
        }
                
        // link the spoke connections 
        pSpoke.setNextSpoke(cSpoke);
        cSpoke.setNextSpoke(pSpoke);
        
        // Activity Monitor
        ScheduledExecutorService sesPrintReport = Executors.newSingleThreadScheduledExecutor();
        sesPrintReport.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                activityMonitor();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public int getTranslatorId() {
        return id;
    }
    public int getHubPort() {
        return hubPort;
    }
        
    private void activityMonitor() {
        if ((cSpoke.getLastActivity() > 0) || (pSpoke.getLastActivity() > 0)) {
            logger.info(String.format("activityMonitor [%d] - Active", id));
            cSpoke.clearActivity();
            pSpoke.clearActivity();
        } else {
            logger.info(String.format("activityMonitor [%d] - No Active", id));
        }
    }
}
