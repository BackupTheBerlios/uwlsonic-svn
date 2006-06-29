package com.sap.uwl.som.portal;

import java.util.List;
import java.util.Map;

import com.sap.netweaver.bc.uwl.*;
import com.sap.netweaver.bc.uwl.config.Action;
import com.sap.netweaver.bc.uwl.connect.*;
import com.sap.security.api.IUser;
import com.sap.security.api.UMException;
import com.sap.security.api.UMFactory;
import com.sap.tc.logging.Location;
import com.sap.uwl.som.provider.SomInboxProvider;
import com.sap.uwl.som.provider.SomInboxProviderException;

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
public class ActionHandler implements IActionHandler {

	private static final Location loc = Location.getLocation(ActionHandler.class);
	private IPushChannel pushChannel;
	private IProviderConnector connector;

	/**
	 * Creates a new instance of the ActionHandler and keeps a reference to its
	 * connector. 
	 * 
	 * @param pc
	 * @param connector
	 */
	public ActionHandler(IPushChannel pc, IProviderConnector connector) {
		this.pushChannel = pc;
		this.connector = connector;
	}

	/**
	 * Returns the ID of the ActionHandler.
	 */
	public String getId() {
		return Action.PROVIDER_ACTION_HANDLER;
	}

	public String getUrl(UWLContext arg0, Item arg1, Action arg2, Map arg3)
		throws UWLException {
		return null;
	}

	public boolean isLauncher() {
		return false;
	}

	public boolean needsItemValidation() {
		return false;
	}

	/**
	 * This method is triggered by the UWL framework. Depending on which actions the connector
	 * is registered on, a certain action is executed and can be handled by this action handler.
	 * 
	 * @return a valid provider status
	 */
	public ProviderStatus performAction(UWLContext context, Item item, Action action, java.util.Map properties)
				throws ConnectorException {
		try {
			String actionName = action.getName();
			if(Action.DELETE.equals(actionName)) {
				try {
					SomInboxProvider somProvider = new SomInboxProvider();
					somProvider.deleteItem(context.getUser(),item.getSystemId(),item.getExternalId());
					pushChannel.deleteItem(connector,context,item);
				} catch (SomInboxProviderException e) {
					loc.errorT("performAction() - Action.DELETE: "+e.toString()+ ", "+e.getMessage());
				}
			}
			else if(Action.MARK_AS_READ.equals(actionName)) {

				try {
					if (!item.getStatus().equals(StatusEnum.READ)) {
						SomInboxProvider somProvider = new SomInboxProvider();
						somProvider.setItemAsRead(context.getUser(),item.getSystemId(),item.getExternalId());
						item.setStatus(StatusEnum.READ);
					}
					pushChannel.updateItem(connector,context,item);
				} catch (SomInboxProviderException e) {
					loc.errorT("performAction() - Action.MARK_AS_READ: "+e.toString()+ ", "+e.getMessage());
				}
			}
			else if(Action.ACTION_FORWARD.equals(actionName)) {
				List users = (List) properties.get("userIdList");
				try {
					IUser[] receivers = new IUser[users.size()];
					for (int i=0;i<receivers.length;i++) {
						receivers[i] = UMFactory.getUserFactory().getUser((String)users.get(i));
					}
					SomInboxProvider somProvider = new SomInboxProvider();
					somProvider.forwardItem(context.getUser(), item.getSystemId(), item.getExternalId(), receivers);					
					pushChannel.updateItem(connector,context,item);
				} catch (SomInboxProviderException e) {
					loc.errorT("performAction() - Action.FORWARD: "+e.toString()+ ", "+e.getMessage());
				} catch (UMException e) {
					loc.errorT("performAction() - Action.FORWARD: "+e.toString()+ ", "+e.getMessage());
				}
			}
		}
		catch(UWLException ue) {
			loc.errorT("performAction(): "+ue.toString()+ ", "+ue.getMessage());
			throw new ConnectorException(ue);
		}
		
		return new ProviderStatus(true, null, Action.PROVIDER_ACTION_HANDLER);
	}

}
