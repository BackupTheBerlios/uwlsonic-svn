package com.sap.uwl.som.provider;

import java.util.Date;

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
public class SomInboxItem {

	private IUser user;
	private String system;
	private String DOCID;
	private String ObjType;
	private String ObjName;
	private String ObjDescription;
	private String ObjLanguage;
	private String OwnerName;
	private String OwnerFullname;
	private String SenderName;
	private String SenderFullname;
	private Date SendDate;
	private String ReceiverName;
	private String ReceiverFullname;
	private int DocSize;
	private boolean Read;
	private byte[] content;
	private SomInboxAttachment[] attachments;
	private int priority;

	/**
	 * @return
	 */
	public String getDOCID() {
		return DOCID;
	}

	/**
	 * @return
	 */
	public int getDocSize() {
		return DocSize;
	}

	/**
	 * @return
	 */
	public String getObjDescription() {
		return ObjDescription;
	}

	/**
	 * @return
	 */
	public String getObjLanguage() {
		return ObjLanguage;
	}

	/**
	 * @return
	 */
	public String getObjName() {
		return ObjName;
	}

	/**
	 * @return
	 */
	public String getObjType() {
		return ObjType;
	}

	/**
	 * @return
	 */
	public String getOwnerFullname() {
		return OwnerFullname;
	}

	/**
	 * @return
	 */
	public String getOwnerName() {
		return OwnerName;
	}

	/**
	 * @return
	 */
	public String getReceiverFullname() {
		return ReceiverFullname;
	}

	/**
	 * @return
	 */
	public String getReceiverName() {
		return ReceiverName;
	}

	/**
	 * @return
	 */
	public Date getSendDate() {
		return SendDate;
	}

	/**
	 * @return
	 */
	public String getSenderFullname() {
		return SenderFullname;
	}

	/**
	 * @return
	 */
	public String getSenderName() {
		return SenderName;
	}

	/**
	 * @param string
	 */
	protected void setDOCID(String string) {
		DOCID = string;
	}

	/**
	 * @param string
	 */
	protected void setDocSize(String string) {
		try {
			DocSize = Integer.parseInt(string);
		} catch (Exception e) {
			DocSize = 0;
		}
	}
	
	protected void setDocSize(int size) {
		DocSize = size;
	}

	/**
	 * @param string
	 */
	protected void setObjDescription(String string) {
		ObjDescription = string;
	}

	/**
	 * @param string
	 */
	protected void setObjLanguage(String string) {
		ObjLanguage = string;
	}

	/**
	 * @param string
	 */
	protected void setObjName(String string) {
		ObjName = string;
	}

	/**
	 * @param string
	 */
	protected void setObjType(String string) {
		ObjType = string;
	}

	/**
	 * @param string
	 */
	protected void setOwnerFullname(String string) {
		OwnerFullname = string;
	}

	/**
	 * @param string
	 */
	protected void setOwnerName(String string) {
		OwnerName = string;
	}

	/**
	 * @param string
	 */
	protected void setReceiverFullname(String string) {
		ReceiverFullname = string;
	}

	/**
	 * @param string
	 */
	protected void setReceiverName(String string) {
		ReceiverName = string;
	}

	/**
	 * @param string
	 */
	protected void setSendDate(Date d) {
		SendDate = d;
	}

	/**
	 * @param string
	 */
	protected void setSenderFullname(String string) {
		SenderFullname = string;
	}

	/**
	 * @param string
	 */
	protected void setSenderName(String string) {
		SenderName = string;
	}

	/**
	 * @return
	 */
	public boolean getRead() {
		return Read;
	}

	/**
	 * @param string
	 */
	protected void setRead(boolean b) {
		Read = b;
	}

	/**
	 * @return
	 */
	public byte[] getContent() {
		return content;
	}

	/**
	 * @param bs
	 */
	protected void setContent(byte[] bs) {
		content = bs;
	}

	public String toString() {
		return this.getDOCID() + ", "+this.getObjName()+", "+this.getObjType();
	}
	
	/**
	 * @return
	 */
	public SomInboxAttachment[] getAttachments() {
		return attachments;
	}

	/**
	 * @param attachments
	 */
	protected void setAttachments(SomInboxAttachment[] attachments) {
		this.attachments = attachments;
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
	protected void setSystem(String string) {
		system = string;
	}

	/**
	 * @param user
	 */
	protected void setUser(IUser user) {
		this.user = user;
	}

	/**
	 * @return
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @param string
	 */
	protected void setPriority(int i) {
		priority = i;
	}
	
	public void setReadFlag() throws SomInboxProviderException {
		this.Read = true;
		new SomInboxProvider().setItemAsRead(
			this.getUser(),
			this.getSystem(),
			this.getDOCID()
		);
	}

}
