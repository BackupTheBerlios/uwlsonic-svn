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
 * @author Lars Rueter, Thilo Brandt, SAP AG
 */
public class Constants {
	

    // Function names
	public static final String FM_SO_ATTACHMENT_READ 	= "SO_ATTACHMENT_READ_API1";
	public static final String FM_SO_USER_READ		 	= "SO_USER_READ_API1";
	public static final String FM_SO_FOLDER_READ_API1 	= "SO_FOLDER_READ_API1";
	public static final String FM_SO_DOCUMENT_READ_API1	= "SO_DOCUMENT_READ_API1";
	public static final String FM_SO_DOCUMENT_SET_STATUS_API1 = "SO_DOCUMENT_SET_STATUS_API1";
	public static final String FM_SO_OLD_DOCUMENT_SEND_API1 = "SO_OLD_DOCUMENT_SEND_API1";
	public static final String FM_SO_DOCUMENT_DELETE_API1 = "SO_DOCUMENT_DELETE_API1";
	
	
	// Attachment types
	public static final String ATTACH_URL="URL";
	public static final String ATTACH_OBJ="OBJ";
	public static final String ATTACH_INT="INT";		//internal doc
	public static final String ATTACH_SCR="SCR";		//raw doc
	public static final String ATTACH_EXT="EXT";		//ext doc: pc app file
	public static final String ATTACH_TXT="TXT";		//txt doc: also pc app file
	public static final String ATTACH_XML="XML";		//xml doc

}
