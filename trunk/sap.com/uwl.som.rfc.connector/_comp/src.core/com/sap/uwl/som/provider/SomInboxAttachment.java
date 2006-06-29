package com.sap.uwl.som.provider;

import java.util.Random;

import com.sap.security.api.IUser;

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
public class SomInboxAttachment {
	
	private IUser user;
	private String system;
	private String attachmentId;
	private String attachmentTitle;
	private String attachmentType;
	private int attachementSize;
	private byte[] content;
	
	/**
	 * @return
	 */
	public String getAttachmentId() {
		return attachmentId;
	}

	/**
	 * @return
	 */
	public String getAttachmentTitle() {
		return attachmentTitle;
	}

	/**
	 * @param string
	 */
	public void setAttachmentId(String string, Random rand) {
		attachmentId = string;
	}

	/**
	 * @param string
	 */
	public void setAttachmentTitle(String string) {
		attachmentTitle = string;
	}

	public byte[] getContent() throws SomInboxProviderException {
		if (this.content==null) {
			this.content = new SomInboxProvider().getAttachmentContent(
				this.getUser(),
				this.getSystem(),
				this.getAttachmentId()
			);
		}
		return this.content;
	}

	/**
	 * @return
	 */
	public String getSystem() {
		return system;
	}

	/**
	 * @return
	 */
	public IUser getUser() {
		return user;
	}

	/**
	 * @param string
	 */
	public void setSystem(String string) {
		system = string;
	}

	/**
	 * @param user
	 */
	public void setUser(IUser user) {
		this.user = user;
	}

	/**
	 * @return
	 */
	public String getAttachmentType() {
		return attachmentType;
	}

	/**
	 * @param string
	 */
	public void setAttachmentType(String string) {
		attachmentType = string;
	}

	/**
	 * @return
	 */
	public int getAttachementSize() {
		return attachementSize;
	}

	/**
	 * @param i
	 */
	public void setAttachementSize(int i) {
		attachementSize = i;
	}

}
