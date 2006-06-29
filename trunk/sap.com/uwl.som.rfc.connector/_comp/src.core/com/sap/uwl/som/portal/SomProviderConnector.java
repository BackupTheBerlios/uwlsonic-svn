package com.sap.uwl.som.portal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import com.sap.netweaver.bc.uwl.Attachment;
import com.sap.netweaver.bc.uwl.IUWLService;
import com.sap.netweaver.bc.uwl.Item;
import com.sap.netweaver.bc.uwl.ItemCollection;
import com.sap.netweaver.bc.uwl.ItemKey;
import com.sap.netweaver.bc.uwl.PriorityEnum;
import com.sap.netweaver.bc.uwl.ProviderStatus;
import com.sap.netweaver.bc.uwl.StatusEnum;
import com.sap.netweaver.bc.uwl.UWLContext;
import com.sap.netweaver.bc.uwl.UWLException;
import com.sap.netweaver.bc.uwl.WorkLog;
import com.sap.netweaver.bc.uwl.config.Action;
import com.sap.netweaver.bc.uwl.config.ItemType;
import com.sap.netweaver.bc.uwl.connect.ConnectorException;
import com.sap.netweaver.bc.uwl.connect.ConnectorFilter;
import com.sap.netweaver.bc.uwl.connect.ConnectorResult;
import com.sap.netweaver.bc.uwl.connect.IActionHandler;
import com.sap.netweaver.bc.uwl.connect.IAttachmentConnector;
import com.sap.netweaver.bc.uwl.connect.IProviderConnector;
import com.sap.tc.logging.Location;
import com.sap.uwl.som.provider.SomInboxAttachment;
import com.sap.uwl.som.provider.SomInboxItem;
import com.sap.uwl.som.provider.SomInboxProvider;
import com.sap.uwl.som.provider.SomInboxProviderException;
import com.sapportals.portal.prt.component.IPortalComponentRequest;

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
 * Class implementing a UWL Provider Connector for SAP Office Mail.
 * 
 * @author Lars Rueter, Thilo Brandt, SAP AG
 */
public class SomProviderConnector implements IProviderConnector, IAttachmentConnector {
	
	private static final Location loc = Location.getLocation(SomProviderConnector.class);

	//Connector Id
	public static final String SOM_CONNECTOR_ID = "SomProviderConnector";
	
	// item types
	public static final String SOM_ITEM_TYPE = "uwl.notification.som";

	private ActionHandler actionHandler = null;
	private IUWLService uwlService = null;

	/**
	 * Creates a new Instance of the UWL connector and keeps a reference 
	 * to the UWL service. Keep in mind to update the reference if UWL is
	 * restarted.
	 * 
	 * @param uwlService current UWL service instance
	 */
	public SomProviderConnector(IUWLService uwlService) {
		this.uwlService = uwlService;
		try {
			this.actionHandler = new ActionHandler(uwlService.getPushChannel(),this);
		}
		catch(UWLException ue) {
			loc.errorT("Constructor(): "+ue.toString()+ ", "+ue.getMessage());
		}
		loc.infoT("Created new instance of SomProviderConnector.");
	}

	/**
	 * Returns a unique ID for this connector implementation.
	 */
	public String getId() {
		return SomProviderConnector.SOM_CONNECTOR_ID;
	}

	/**
	 * Return wether a certain item type is supported or not. This connector only
	 * supports a single item type.
	 */
	public boolean supportsItemType(String type) {
		return ItemType.matchType(SOM_ITEM_TYPE, type);
	}

	public boolean supportsCacheClearance(String arg0) {
		return true;
	}

	/** 
	 * Resturns the list of items the backend system is providing. 
	 * This implementation ignores ConnectorFilter. It simple returns all items
	 * gathered by the connector.
	 * 
	 * @return a valid ConnectorResult object containing the item list and a valid status.
	 */
	public ConnectorResult getItems(UWLContext context,	String itemType, ConnectorFilter connectorFilter, String system)
		throws ConnectorException {

		 ConnectorResult result = null;
		 List items = null;
		 if(ItemType.matchType(SOM_ITEM_TYPE, itemType)){
			try {
				SomInboxProvider somProvider = new SomInboxProvider();
				List somItems = somProvider.getAllItems(context.getUser(), system);
				items = mapSomToUwlItems(context, itemType, connectorFilter, system, somItems);
			} catch (SomInboxProviderException e) {
				loc.errorT("getItems(): "+e.toString()+ ", "+e.getMessage());
				throw new ConnectorException(e);
			}
		 }
		
		 ProviderStatus  status = new ProviderStatus(true, system, SomProviderConnector.SOM_CONNECTOR_ID);
		 return ConnectorResult.createSnapshotResult(new ItemCollection(items), status);
	}

	/**
	 * This method populated hollow items which were not fetched 
	 * on the first request. 
	 * 
	 * @return true if population was successful.
	 */
	public boolean populateHollowItem(UWLContext ctx, Item item)
		throws ConnectorException {
			
		SomInboxProvider somProvider = new SomInboxProvider();
		try {
			SomInboxItem entry =
				somProvider.getItem(ctx.getUser(), item.getSystemId(), item.getExternalId());
			
			item.setAttachmentCount((entry.getAttachments()!=null ? entry.getAttachments().length : Item.ZERO_NUMBER_ATTACHMENT));
			uwlService.getPushChannel().updateItem(this, ctx, item);
			return true;
		} catch (SomInboxProviderException e) {
			loc.errorT(e.toString()+", "+e.getMessage());
		} catch (UWLException e) {
			loc.errorT(e.toString()+", "+e.getMessage());
		}
		return false;
	}

	/**
	 * Returns the description of the passsed item.
	 * Keep in mind, that there are items in SAP Office Mail, that do not expose descriptions.
	 * 
	 * @return a valid description or empty string, if no description was found.
	 */
	public String getDescription(Item item, UWLContext ctx)
		throws ConnectorException {
		
		SomInboxProvider somProvider = new SomInboxProvider();
		try {
			SomInboxItem entry =
				somProvider.getItem(ctx.getUser(), item.getSystemId(), item.getExternalId());	
				
			if (item.isHollow()) {
				this.populateHollowItem(ctx, item);
			}
			return new String(entry.getContent());
		} catch (SomInboxProviderException e) {
			loc.infoT("No description for item: "+item.getSubject());
		} catch (UWLException e) {
			loc.errorT(e.toString()+", "+e.getMessage());
		}
		return "";
	}
	
	/**
	 * Returns the map with all supported exception.
	 * This connector does not provide additional action except the standard ones.
	 * 
	 * @return a map with all supported actions
	 */
	public Map getAllActionsForItem(UWLContext ctx, Map currentActions, Item item)
		throws ConnectorException {
		return currentActions;
	}

	/**
	 * Return an instance of an ActionHandler. 
	 */
	public IActionHandler getActionHandler(String actionHandlerId) {
		if(Action.PROVIDER_ACTION_HANDLER.equals(actionHandlerId))
			return actionHandler;
		
		return null;
	}

	/**
	 * Checks wether an action is valid for a certain item or not.
	 * This connector does no action validation.
	 */
	public boolean isActionValidForItem(UWLContext ctx, Item item, Action action) {
		return true;
	}

	/**
	 * Checks wether an item is valid or not.
	 * This connector does no item validation.
	 */
	public boolean isItemValid(UWLContext ctx, Item item)
		throws ConnectorException {
		return true;
	}

	/**
	 * This method is called to fetch the current item state, before executing an action on the item.
	 * This connector return the cached item from the UWL Cache.
	 * 
	 * @return the requested item from the cache or null if it does not exists.
	 */
	public Item getItem(UWLContext ctx, String systemId, String externalId)
		throws ConnectorException {
		try {
			return uwlService.getPushChannel().getItemByKey(new ItemKey(ctx.getUserId(),SomProviderConnector.SOM_CONNECTOR_ID, systemId, externalId));
		}
		catch(UWLException ue) {
			throw new ConnectorException(ue);
		}	
	}

	/**
	 * This method provide Attachment header data for a certain item
	 * 
	 * @return an array of attachments
	 */
	public Attachment[] getAttachmentHeaders(UWLContext ctx, Item item)
		throws ConnectorException {
			
		SomInboxProvider somProvider = new SomInboxProvider();
		try {
			SomInboxItem entry = somProvider.getItem(ctx.getUser(), item.getSystemId(), item.getExternalId());
			SomInboxAttachment[] somAttachments = entry.getAttachments();
			if (somAttachments!=null && somAttachments.length>0) {
				Attachment[] a = new Attachment[somAttachments.length];
				for (int i=0;i<a.length;i++) {
					String attachmentType = somAttachments[i].getAttachmentType().toLowerCase();
					a[i] = new Attachment(SomProviderConnector.SOM_CONNECTOR_ID, 
										  (attachmentType.equalsIgnoreCase("url") ? Attachment.TYPE_URL : Attachment.TYPE_MIME_DATA), 
										  somAttachments[i].getAttachmentTitle(),
										  "",
										  somAttachments[i].getAttachmentId(),
										  item.getCreatorId(),
										  null, // creation date null will correctly 
										  		// display multible attachments  
										  item.getPriority(),
										  somAttachments[i].getAttachmentTitle(),
										  attachmentType,
										  this.getMimeType(ctx, attachmentType),
										  somAttachments[i].getAttachementSize()
										  );
				}
				return a;
			}
		} catch (SomInboxProviderException e) {
			loc.errorT(e.toString()+", "+e.getMessage());
		}
			
		return Item.EMPTY_ATTACHMENTS;
	}

	/**
	 * This method populates the attachment content for the given item and attachment header.
	 * This connector simple delegates the call to mass operation.
	 */
	public void populateAnAttachment(UWLContext ctx, Item item, Attachment attachment) throws ConnectorException {
		this.populateAttachments(ctx, item, new Attachment[]{attachment} );
	}

	/**
	 * This method populates the attachment content for the given item and attachment headers.
	 */
	public void populateAttachments(UWLContext ctx, Item item, Attachment[] attachments) throws ConnectorException {
		SomInboxProvider somProvider = new SomInboxProvider();
		byte[] byteContent = null;
		try {
			SomInboxItem entry = somProvider.getItem(ctx.getUser(), item.getSystemId(), item.getExternalId());
			SomInboxAttachment[] somAttachments = entry.getAttachments();
			if (somAttachments!=null && somAttachments.length>0) {
				String somId = null;
				String attId = null;
				for (int i=0;i<somAttachments.length;i++) {
					somId = somAttachments[i].getAttachmentId();
					for (int j=0;j<attachments.length;j++){
						attId = attachments[j].getInternalId();
						if (somId.equalsIgnoreCase(attId)) {
							try {
								byteContent = somAttachments[i].getContent();
								if (attachments[j].getType() == Attachment.TYPE_URL) {
									attachments[j].setContent(getUrlFromByteArray(byteContent));
								} else {
									attachments[j].setContent(byteContent);
								}
							} catch (UWLException ex) {
								loc.errorT(ex.toString()+", "+ex.getMessage());
							}
						}
					}
				}			
			}
		} catch (SomInboxProviderException e) {
			loc.errorT(e.toString()+", "+e.getMessage());
		}
	}	

	/**
	 * Checks wether the specified system need a remote connection.
	 * This connector always needs a remote connection to the SAP System.
	 */
	public boolean needsRemoteConnection(String system) {
		return true;
	}

	public boolean isLogRetrievalSupported(UWLContext ctx, Item item) {
		return false;
	}

	public WorkLog getLogData(UWLContext ctx, Item item)
		throws ConnectorException {
		return null;
	}

	public WorkLog getLogData(Locale locale, String arg1)
		throws ConnectorException {
		return null;
	}
	
	/**
	 * This method maps the item data from the SAP system to the UWL item representation.
	 * This connector does not validate connector filters.
	 * 
	 * @param context the current UWL context
	 * @param itemType the item type
	 * @param qp a connector filter
	 * @param system system id of the backend
	 * @param somItems items of the SAP backend system
	 * 
	 * @return a List of UWL items
	 */
	private List mapSomToUwlItems (UWLContext context, String itemType, ConnectorFilter qp, String system, List somItems) {
		List retItems = new ArrayList();	
		SomInboxItem entry = null;			
		Item uwlItem = null;
				
		for(int j=0; j<somItems.size(); j++) {
			entry = (SomInboxItem)somItems.get(j);
			uwlItem = new Item(SomProviderConnector.SOM_CONNECTOR_ID, 	//connectorId
								system,							//systemId
								entry.getDOCID(),				//externalId
								entry.getUser().getUniqueID(),	//userId
								(entry.getAttachments()!=null ? entry.getAttachments().length : Item.UNKNOWN_ATTACHMENT_EXISTENCE), 	//attachment count
								entry.getSendDate(),			//date created
								entry.getSenderFullname(),		//creator id
								null,							//due date
								null,							//external object id
								SOM_ITEM_TYPE,					//external type
								SOM_ITEM_TYPE,					//item type
								PriorityEnum.getEnumFromInt(entry.getPriority()),	//priority
								(entry.getRead())? StatusEnum.READ : StatusEnum.NEW,//status
								entry.getObjDescription() );	//subject
			
			uwlItem.setHollow(true);
			retItems.add(uwlItem);
		}

		return retItems;								
	}
	
	private String getMimeType(UWLContext context, String attType){
		Object request=context.getOriginRequest();
		ServletContext ctx=null;
		if(request instanceof IPortalComponentRequest){
			ctx= ((IPortalComponentRequest)request).getServletConfig().getServletContext();
		}else
		if(request instanceof HttpServletRequest){
			ctx=((HttpServletRequest)request).getSession().getServletContext();	
		}
		String mimeType= ctx==null?null:ctx.getMimeType(attType);
		return mimeType;	
	}
	
	private String getUrlFromByteArray(byte[] bContent) {
		String sContent = new String(bContent).toLowerCase().trim();
		return sContent.substring(sContent.indexOf("http://"));
	}
										

}
