package eu.arrowhead.core.translator.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FiwareService {

    //=================================================================================================
    // members
    private final Logger logger = LogManager.getLogger(FiwareService.class);

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------
    public void start() {
        logger.info("Starting FIWARE Service");
    }
    
    
    //-------------------------------------------------------------------------------------------------
    /*@Scheduled(fixedRate = 5000)
    public void testScheduling() {
        logger.info("The time is now {}", new SimpleDateFormat("HH:mm:ss").format(new Date()));
    }*/
}
