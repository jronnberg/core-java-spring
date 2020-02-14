package eu.arrowhead.core.translator.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PluginService {

    //=================================================================================================
    // members
    private final Logger logger = LogManager.getLogger(PluginService.class);

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------
    public void start() {
        logger.info("Starting Plugin Service");
    }
}
