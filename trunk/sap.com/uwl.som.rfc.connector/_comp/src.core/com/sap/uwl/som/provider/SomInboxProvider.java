package com.sap.uwl.som.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.resource.cci.MappedRecord;

import com.sap.security.api.IUser;
import com.sap.security.api.UMException;
import com.sap.security.api.UMFactory;
import com.sap.security.api.umap.IUserMapping;
import com.sap.security.api.umap.system.ExceptionInImplementationException;
import com.sap.security.api.umap.system.ISystemLandscapeObject;
import com.sap.security.api.umap.system.ISystemLandscapeWrapper;
import com.sap.tc.logging.Location;
import com.sapportals.connector.ConnectorException;
import com.sapportals.connector.execution.structures.IAbstractRecord;
import com.sapportals.connector.execution.structures.IRecord;
import com.sapportals.connector.execution.structures.IRecordSet;
import com.sapportals.portal.ivs.cg.ConnectionProperties;


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
 * This class represents a bean which encapsulates the 
 * WebService calls to the SAP System.
 * 
 * @author Lars Rueter, Thilo Brandt, SAP AG
 */
public class  SomInboxProvider {

	private static final Location loc = Location.getLocation(SomInboxProvider.class);

	private boolean m_useCache;
	
	private Map m_cacheMap;

	/**
	 * Creates a new Instance of the SAP Office Mail provider and keeps a reference 
	 * to the SAP backend.
	 * This constructor implicitly switches off the local caching.
	 */
	public SomInboxProvider() {
	}
	
	protected synchronized byte[] getAttachmentContent(IUser user, String system, String attachmentId) throws SomInboxProviderException {
		
		loc.infoT("Getting attachment: "+attachmentId+", System: "+system+", User: "+user.getUniqueID());

		SimpleTransaction t = null;
		try {
			t =
				new SimpleTransaction(
					system,	new ConnectionProperties(Locale.getDefault(), user));
					
			IRecordSet content = null;
		
			MappedRecord ipList = t.createInput();
			ipList.put("ATTACHMENT_ID", attachmentId);

			MappedRecord opList = t.executeFunction(Constants.FM_SO_ATTACHMENT_READ);

			IRecord header = (IRecord) opList.get("ATTACHMENT_DATA");
			
			String size = header.getString("ATT_SIZE").trim();
			int docSize = 0;
			if (size!=null && size.length()>0) {
				docSize = Integer.parseInt(size);
			}

			String docType = header.getString("ATTACH_TYP").trim();

			if (docType.equals(Constants.ATTACH_XML) || 
				docType.equals(Constants.ATTACH_TXT) ||
				docType.equals(Constants.ATTACH_URL)) {
					content = (IRecordSet) opList.get("ATTACHMENT_CONTENT");
			} else {
				content = (IRecordSet) opList.get("CONTENTS_HEX");
			}

			ByteArrayOutputStream contentByte =
				new ByteArrayOutputStream(docSize);
			content.beforeFirst();
			while (content.next()) {
				contentByte.write(content.getBytes("LINE"));
			}
			contentByte.flush();

			byte[] result = contentByte.toByteArray();
			contentByte.close();

			return result;
		} catch (Exception e) { //com.sapportals.connector.ConnectorException or JCO.Exception
				loc.warningT(
					"Unable to retrieve SAP office data for attachment in system "
						+ system);
				loc.errorT(e.toString() + ", " + e.getMessage());				
		} finally {
			if (t!=null)
				t.end();
		}

		return new byte[0];

	}
	
	/**
	 * Returns a list with all SAP Office Mail items for a certain user and a certain system.
	 * 
	 * @param user IUser for which the items should be fetched.
	 * @param system system id for which the items should be fetched.
	 * @return a list with SomInboxItem objects
	 * @throws SomInboxProviderException
	 */
	public synchronized List getAllItems(IUser user, String system) throws SomInboxProviderException {
		List items = new ArrayList();
		String folderId = this.getInboxFolderId(user, system);
		
		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
					system,	new ConnectionProperties(Locale.getDefault(), user));
					
			MappedRecord ipList = t.createInput();
			ipList.put("FOLDER_ID", folderId);
	
			MappedRecord opList = t.executeFunction(Constants.FM_SO_FOLDER_READ_API1);
		
			IRecordSet somItemTable = (IRecordSet) opList.get("FOLDER_CONTENT");
			somItemTable.beforeFirst();
			SomInboxItem item = null;
			while (somItemTable.next()) {
				item = getDocumentMetadata(somItemTable);
				
				item.setUser(user);
				item.setSystem(system);

				if (loc.beInfo())
					loc.infoT("Adding item: "+item.toString());
		
				items.add(item);
			}
			
		} catch (ConnectorException e) {
			loc.errorT(e.toString() + ", "+ e.getMessage());
			throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_FOLDER, e.toString()+ ", "+ e.getMessage());
		} finally {
			if (t!=null)
				t.end();
		}
	
		return items;
	}

	/**
	 * Sets a single SAP Office Mail item to the status READ.
	 * 
	 * @param user IUser the item belongs to
	 * @param system System the item is stored in
	 * @param itemId the item id of the item the status hsould be changed
	 * @throws SomInboxProviderException
	 */
	public synchronized void setItemAsRead(IUser user, String system, String itemId) throws SomInboxProviderException {
		loc.infoT("Set SOM Item as read: "+itemId+", System: "+system+", User: "+user.getUniqueID());
		
		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
					system,	new ConnectionProperties(Locale.getDefault(), user));
					
			MappedRecord ipList = t.createInput();
			ipList.put("DOCUMENT_ID", itemId);	
			ipList.put("STATUS", "READ");		
			MappedRecord opList = t.executeFunction(Constants.FM_SO_DOCUMENT_SET_STATUS_API1);
								
						
		} catch (Exception e) { 
				loc.warningT(
					"Unable to set read flag for SAP office item in system " + system);
				loc.errorT(e.toString() + ", " + e.getMessage());				
		} finally {
			if (t!=null)
				t.end();
		}				
	}

	/**
	 * Forwards a single item to a list of receivers.
	 * 
	 * @param user IUser the item belongs
	 * @param system Systenm the item is stored in
	 * @param itemId item id of the fowarding item
	 * @param receivers array of IUsers the item should be sent to
	 * @throws SomInboxProviderException
	 */
	public synchronized void forwardItem(IUser user, String system, String itemId, IUser[] receivers) throws SomInboxProviderException {
		loc.infoT("Forwarding SOM Item: "+itemId+", System: "+system+", User: "+user.getUniqueID());
		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
					system,	new ConnectionProperties(Locale.getDefault(), user));
					
			MappedRecord ipList = t.createInput();
			ipList.put("DOCUMENT_ID", itemId);	
			ipList.put("PUT_IN_OUTBOX", "X");	
	
			
			IRecordSet ipUserTable = t.createNewRecordSetInput(Constants.FM_SO_OLD_DOCUMENT_SEND_API1, "RECEIVERS");
			ipUserTable.beforeFirst();
		
			for (int i=0;i<receivers.length;i++) {
				ipUserTable.insertRow();
				ipUserTable.next();	
				
//				IUserMappingData recv_umd = this.getMapping(receivers[i], system);
//				if (recv_umd==null) throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_DOCUMENT, "No user mapping found for user: "+receivers[i].getUniqueID());
				ipUserTable.setString("RECEIVER", this.getR3User(receivers[i], system));
			}
			
			ipList.put("RECEIVERS", ipUserTable);
			
			MappedRecord opList = t.executeFunction(Constants.FM_SO_OLD_DOCUMENT_SEND_API1);
		} catch (Exception e) { 
				loc.warningT(
					"Unable to forward SAP office item " + itemId + "  in system " + system);
				loc.errorT(e.toString() + ", " + e.getMessage());				
		} finally {
			if (t!=null)
				t.end();
		}
	}

	/**
	 * Deletes a single SAP Office Mail item.
	 * 
	 * @param user IUser the item belongs to
	 * @param system System the item is stored in
	 * @param itemId item to be deleted
	 * @throws SomInboxProviderException
	 */
  	public synchronized void deleteItem(IUser user, String system, String itemId) throws SomInboxProviderException {
  		
		loc.infoT("Deleting SOM Item: "+itemId+", System: "+system+", User: "+user.getUniqueID());

		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
				system,	new ConnectionProperties(Locale.getDefault(), user));
				
			MappedRecord ipList = t.createInput();
			ipList.put("DOCUMENT_ID", itemId);
			ipList.put("UNREAD_DELETE", "X");
			ipList.put("PUT_IN_TRASH", "X");
	
			MappedRecord opList = t.executeFunction(Constants.FM_SO_DOCUMENT_DELETE_API1);
			
		} catch (ConnectorException e) {
			loc.errorT(e.toString() + ", "+ e.getMessage());
			throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_FOLDER, e.toString()+ ", "+ e.getMessage());
		} finally {
			if (t!=null)
				t.end();
		}
	}
	
	/**
	 * Gets a single SAP Office Mail item. Keep in mind that the attachment content is not yet fetched with this
	 * method. t will be called by the SomInboxAttachment.getContent() call as a callback.
	 * 
	 * @param user IUser the item belongs to
	 * @param system System the item is stored in
	 * @param itemId item to be retrieved
	 * @return a valif item or throws Exception if not found
	 * @throws SomInboxProviderException
	 */
	public synchronized SomInboxItem getItem(IUser user, String system, String itemId) throws SomInboxProviderException {

		loc.infoT("Getting SOM Item: "+itemId+", System: "+system+", User: "+user.getUniqueID());

		SomInboxItem currentItem = null;
		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
				system,	new ConnectionProperties(Locale.getDefault(), user));
				
			MappedRecord ipList = t.createInput();
			ipList.put("DOCUMENT_ID", itemId);
	
			MappedRecord opList = t.executeFunction(Constants.FM_SO_DOCUMENT_READ_API1);
			IRecord opDocData = (IRecord) opList.get("DOCUMENT_DATA");
			
			currentItem = new SomInboxItem();	
			currentItem = getDocumentMetadata(opDocData);
			currentItem.setUser(user);
			currentItem.setSystem(system);
			
			// get document content
			IRecordSet contentTable = (IRecordSet) opList.get("OBJECT_CONTENT");
			byte[] content = getDocumentContent(contentTable, currentItem);
			currentItem.setContent(content);
			
			// get attachment metadata	
			IRecordSet attachmentTable = (IRecordSet) opList.get("ATTACHMENT_LIST");
			SomInboxAttachment[] somAttachments = getAttachmentMetadata(attachmentTable, currentItem);
			currentItem.setAttachments(somAttachments);
			
			loc.infoT("Current item: "+currentItem.toString());
			return currentItem;
			
		} catch (ConnectorException e) {
			loc.errorT(e.toString() + ", "+ e.getMessage());
			throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_FOLDER, e.toString()+ ", "+ e.getMessage());
		} finally {
			if (t!=null)
				t.end();
		}	
	}
	
	private SomInboxAttachment[] getAttachmentMetadata(IRecordSet attachmentTable, SomInboxItem currentItem) throws SomInboxProviderException {
		Random rand = new Random();
		List attachmentList = new ArrayList();
		try {
			while (attachmentTable.next()) {
				SomInboxAttachment somAttachment = new SomInboxAttachment();
				somAttachment.setUser(currentItem.getUser());
				somAttachment.setSystem(currentItem.getSystem());
				somAttachment.setAttachmentId(attachmentTable.getString("ATTACH_ID"), rand);
				somAttachment.setAttachmentTitle(attachmentTable.getString("ATT_DESCR"));
				somAttachment.setAttachmentType(attachmentTable.getString("ATTACH_TYP"));
				String size = attachmentTable.getString("ATT_SIZE").trim();
				if (size!=null && size.length()>0) {
						somAttachment.setAttachementSize(Integer.parseInt(size));
				}
				
				loc.infoT("Adding attachment: "+somAttachment.getAttachmentId()+", System: "+currentItem.getSystem()+", User: "+currentItem.getUser().getUniqueID());
				attachmentList.add(somAttachment);
			}
		} catch (ConnectorException e) {
			loc.errorT(e.toString() + ", "+ e.getMessage());
			throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_USER, e.toString()+ ", "+ e.getMessage());
		}
		
		// convert list of attachments to array of attachments
		SomInboxAttachment[] somAttachments = 
			(SomInboxAttachment[]) attachmentList
					.toArray(new SomInboxAttachment[attachmentList.size()]);
		
		return somAttachments;
	}

	private SomInboxItem getDocumentMetadata(IAbstractRecord somItem) throws ConnectorException {
		SomInboxItem item = new SomInboxItem();
		item.setDOCID(somItem.getString("DOC_ID"));
		item.setDocSize(somItem.getString("DOC_SIZE"));
		item.setObjDescription(somItem.getString("OBJ_DESCR"));
		item.setObjLanguage(somItem.getString("OBJ_LANGU"));
		item.setObjName(somItem.getString("OBJ_NAME"));
		item.setObjType(somItem.getString("OBJ_TYPE"));
		item.setOwnerFullname(somItem.getString("OWNER_FNAM"));
		item.setOwnerName(somItem.getString("OWNER_NAM"));
		item.setReceiverFullname(somItem.getString("REC_FNAM"));
		item.setReceiverName(somItem.getString("REC_NAM"));

		String dt = somItem.getString("SEND_DATE") + " " + somItem.getString("SEND_TIME");
		Date sendDate = new Date(System.currentTimeMillis());
		try {
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			sendDate = df.parse(dt);
		} catch (ParseException pe) {
			loc.errorT(pe.toString());
		}
		item.setSendDate(sendDate);
		item.setSenderFullname(somItem.getString("SEND_FNAM"));
		item.setSenderName(somItem.getString("SEND_NAM"));
		item.setRead(somItem.getString("READ").trim().length()>0);
		item.setPriority(this.normalizePriority(somItem.getInt("PRIORITY")));
		return item;
	}
	
	private byte[] getDocumentContent(IRecordSet contentTable, SomInboxItem currentItem) throws ConnectorException {
		ByteArrayOutputStream contentByte =	new ByteArrayOutputStream(currentItem.getDocSize());
		contentTable.beforeFirst();
		try {
			byte[] line;
			byte[] lf = {(byte) 10, (byte) 13};
			while (contentTable.next()) {
				line = contentTable.getBytes("LINE");
				contentByte.write(line);
				contentByte.write(lf); //add LF after each line. 
			}
			contentByte.flush();
			byte[] result = contentByte.toByteArray();
			contentByte.close();
			return result;
		} catch (IOException e) {
			loc.warningT(
				"Unable to assign the document content.");
			loc.errorT(e.toString() + ", " + e.getMessage());	
		}
		return new byte[0];
	}
	
	private String getInboxFolderId(IUser user, String system) throws SomInboxProviderException {
		SimpleTransaction t = null;
		try {
			t =	new SimpleTransaction(
					system,	new ConnectionProperties(Locale.getDefault(), user));
					
			//IUserMappingData recv_umd = this.getMapping(user, system);
			//if (recv_umd==null) throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_DOCUMENT, "No user mapping found for user: "+user.getUniqueID());
					
			MappedRecord ipList = t.createInput();
			IRecord ipUser = t.createNewRecordInput(Constants.FM_SO_USER_READ, "USER");
			ipUser.setString("SAPNAME", this.getR3User(user, system));
			ipList.put("USER", ipUser);
			
			MappedRecord opList = t.executeFunction(Constants.FM_SO_USER_READ); 

			IRecord opUserData = (IRecord) opList.get("USER_DATA");
			return opUserData.getString("INBOXFOL").trim();

		} catch (Exception e) {
			loc.errorT(e.toString() + ", "+ e.getMessage());
			throw new SomInboxProviderException(SomInboxProviderException.FLAVOR_USER, e.toString()+ ", "+ e.getMessage());
		} finally {
			if (t!=null)
				t.end();
		}	
	}
	
	private int normalizePriority(int p) {
		switch (p) {
			case 1: return 4;
			case 2: 
			case 3: 
			case 4: return 3;
			case 5: 
			case 6: 
			case 7: return 2;
			default: return 1;
		}
	}
	
	private String getR3User(IUser usr, String system) throws SomInboxProviderException {
		if (system==null) {
			try {
				// Get the mapped user ID for the SAP reference system 
				// (i.e. the mapped user ID that is contained in SAP logon tickets)
				return UMFactory.getUserMapping().getR3UserName(usr, null, false);
			} catch (UMException e){
				throw new SomInboxProviderException("", "Unable to retrieve an R/3 user name for this user: " + usr.getUniqueID());		
			}
		}
		
		// Usermapping is requested
		ArrayList systemLandscapes = UMFactory.getSystemLandscapeWrappers();
		ISystemLandscapeWrapper systemLandscape =
			(ISystemLandscapeWrapper) systemLandscapes.get(0);

		try {
			ISystemLandscapeObject lo = systemLandscape.getSystemByAlias(system);
			if (lo!=null) {
				loc.infoT("System definition attributes: "+lo.getAttribute(IUserMapping.UMAP_USERMAPPING_FIELDS));
				return UMFactory.getUserMapping().getR3UserName(usr, lo, false);
			}
		} catch (ExceptionInImplementationException e) {
			
		} catch (UMException e) {
			
		}
		throw new SomInboxProviderException("", "Unable to retrieve an R/3 user name for this user: " + usr.getUniqueID());		
	}	
	
}
