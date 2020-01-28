package eu.arrowhead.core.translator;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import eu.arrowhead.common.ApplicationInitListener;
import eu.arrowhead.common.core.CoreSystemService;

@Component
public class TranslatorApplicationInitListener extends ApplicationInitListener {
	
	//=================================================================================================
	// members
	
	
	@Autowired
	private ApplicationContext applicationContext;
        
	//=================================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------------------
	
	
	//=================================================================================================
	// assistant methods
    
        
        //-------------------------------------------------------------------------------------------------
	@Override
	protected List<CoreSystemService> getRequiredCoreSystemServiceUris() {

		return List.of(CoreSystemService.AUTH_CONTROL_SUBSCRIPTION_SERVICE);
	}
        
        //-------------------------------------------------------------------------------------------------
	@Override
	protected void customInit(final ContextRefreshedEvent event) {
		logger.debug("customInit started...");

	}
	
	//-------------------------------------------------------------------------------------------------
	@Override
	protected void customDestroy() {
		logger.debug("customDestroy started...");
		
	}
    
}