package com.sap.uwl.som.provider;

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
 * @author Thilo Brandt, SAP AG
 */
public class SomInboxProviderException extends Exception {

	public static final String FLAVOR_USER = "Exception on USER:";
	
	public static final String FLAVOR_FOLDER = "Exception on FOLDER:";
	
	public static final String FLAVOR_DOCUMENT = "Exception on DOCUMENT:";

	public static final String FLAVOR_ATTACHMENT = "Exception on ATTACHMENT:";
	
	public SomInboxProviderException(String flavor, String msg) {
		super(flavor + msg);
	}
	
	public SomInboxProviderException(String flavor, Throwable t) {
		super(flavor, t);
	}
}
