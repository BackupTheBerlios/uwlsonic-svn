package com.sap.uwl.som.portal;
 
import com.sap.netweaver.bc.uwl.IUWLService;
import com.sap.netweaver.bc.uwl.UWLException;
import com.sap.tc.logging.Location;
import com.sapportals.portal.prt.runtime.PortalRuntime;
import com.sapportals.portal.prt.service.IServiceContext;

/**
 * Copyright (c) 2006 by SAP AG. All Rights Reserved.
 *
 * SAP, mySAP, mySAP.com and other SAP products and
 * services mentioned herein as well as their respective
 * logos are trademarks or registered trademarks of
 * SAP AG in Germany and in several other countries all
 * over the world. MarketSet and Enterprise Buyer are
 * jointly owned trademarks of SAP AG and Commerce One.
 * All other product and service names mentioned are
 * trademarks of their respective companies.
 * 
 * Class implementing a UWL ActionHandler for SAP Office Mail.
 * 
 * @author Lars Rueter, Thilo Brandt, SAP AG
 */
public class SomRegistrationService implements ISomRegistrationService{

  private IServiceContext mm_serviceContext;
  private SomProviderConnector connector = null;
  private static final Location loc = Location.getLocation(SomRegistrationService.class);
  

  /**
  * Generic init method of the service. Will be called by the portal runtime.
  * @param serviceContext
  */
  public void init(IServiceContext serviceContext)
  {
    mm_serviceContext = serviceContext;
  }

  /**
  * This method is called after all services in the portal runtime
  * have already been initialized.
  */
  public void afterInit()  
  {
	try{
		loc.infoT("Starting up SomRegistrationService...");
		
		//look up the UWLService and register the som provider with it.
		IUWLService uwlService = (IUWLService)PortalRuntime.getRuntimeResources().getService(IUWLService.ALIAS_KEY);	 
		
		//create the connector instance
		connector = new SomProviderConnector(uwlService);
		
		//register with UWL
		uwlService.registerProviderConnector(connector);
		uwlService.registerAttachmentConnector(connector.getId(), connector);
		
		loc.infoT("Startup for SomRegistrationService completed!");
	}
	catch(UWLException ue) {
		loc.errorT("afterInit(): "+ue.toString()+ ", "+ue.getMessage());
	}
  }

  /**
  * configure the service
  * @param configuration
  * @deprecated
  */
  public void configure(com.sapportals.portal.prt.service.IServiceConfiguration configuration)
  {
  }

  /**
  * This method is called by the portal runtime
  * when the service is destroyed.
  */
  public void destroy()
  {
  }

  /**
  * This method is called by the portal runtime
  * when the service is released.
  * @deprecated
  */
  public void release()
  {
  }

  /**
  * @return the context of the service, which was previously set
  * by the portal runtime
  */
  public IServiceContext getContext()
  {
    return mm_serviceContext;
  }

  /**
  * This method should return a string that is unique to this service amongst all
  * other services deployed in the portal runtime.
  * @return a unique key of the service
  */
  public String getKey()
  {
    return KEY;
  }

}
