package com.sap.uwl.som.provider;

import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

import com.sap.tc.logging.Location;
import com.sapportals.connector.ConnectorException;
import com.sapportals.connector.connection.IConnection;
import com.sapportals.connector.execution.ExecutionException;
import com.sapportals.connector.execution.functions.IInteraction;
import com.sapportals.connector.execution.functions.IInteractionSpec;
import com.sapportals.connector.execution.structures.IAbstractRecord;
import com.sapportals.connector.execution.structures.IRecord;
import com.sapportals.connector.execution.structures.IRecordSet;
import com.sapportals.connector.metadata.CapabilityNotSupportedException;
import com.sapportals.connector.metadata.functions.FunctionNotFoundException;
import com.sapportals.connector.metadata.functions.IFunction;
import com.sapportals.connector.metadata.functions.ParameterNotFoundException;
import com.sapportals.connector.metadata.structures.IStructure;
import com.sapportals.portal.ivs.cg.ConnectionProperties;
import com.sapportals.portal.ivs.cg.IConnectorGatewayService;
import com.sapportals.portal.ivs.cg.IConnectorService;
import com.sapportals.portal.prt.runtime.PortalRuntime;

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
 * @author Kiran Gangadharappa, Lars Rueter, Thilo Brandt, SAP AG
 */
public class SimpleTransaction {
	
	private IInteraction ix = null;
	private IInteractionSpec ixspec = null;
	private IConnection connection = null;
	private final ConnectionProperties connectionProperties;
	private final String system;
	private MappedRecord input = null;
	private MappedRecord output =null;
	
	private final static IConnectorGatewayService cgService = (IConnectorGatewayService)PortalRuntime.getRuntimeResources().getService( IConnectorService.KEY);
	private com.sap.mw.jco.JCO.Client nativeClient;
	private String jcoLanguage;

	
	private static Location location= Location.getLocation(SimpleTransaction.class.getName());
	private final boolean debug= location.beDebug();
	
	private Throwable stackTrace=null;
	
	public SimpleTransaction(String destination, ConnectionProperties cp)	{
		connectionProperties=cp;
		system = destination;
		if(debug){
			stackTrace = new Exception(" JCO Transaction Creation stack trace");
		}
	}
	
	public void begin()throws ConnectorException{
		if(connection!=null){
			return;
		}
		try {
			connection = cgService.getConnection(system, connectionProperties);
			Object obj = connection.retrieveNative().getNative("com.sap.mw.jco.JCO.Client");
			nativeClient = (com.sap.mw.jco.JCO.Client) obj;
			jcoLanguage = nativeClient.getAttributes().getLanguage();
			if(debug) 
				location.debugT(" Opened Connection in language " + jcoLanguage);
		} catch (Exception e) {
			location.debugT(" Couldn't Open Connection");
			String mesg=null;
			try {
				String uid =connectionProperties.getUser().getUniqueID();
				String mappedUser= null; //UserManagementMap.getInstanceIntl().mapToExternalUser(uid,system);
				if(mappedUser==null){
					mesg=" No User mapping set up for user "+uid+ " system :"+system;
					location.warningT(mesg);							
				}
				if(debug){
					location.debugT(" Mapped user for :"+uid+" for system "+system+ " is "+mappedUser);
				}
			} catch (Exception  ex) {
				
			}
			if(mesg!=null){
				throw  new com.sapportals.connector.ConnectorException(mesg);
			}else{
				location.warningT(" Getting Connection to :"+ system +" failed for user "+connectionProperties.getUser().getUniqueID());
				
				throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
			}
		}
		try {
			ix = connection.createInteractionEx();
			ixspec = ix.getInteractionSpec();	
		} catch (CapabilityNotSupportedException e) {
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		} catch (com.sapportals.connector.ConnectorException e) {
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		}
	}
	
	public MappedRecord createInput() throws com.sapportals.connector.ConnectorException{
		begin();
		
		if (ix == null) {
			throw new com.sapportals.connector.ConnectorException("Illegal state: Interraction is closed");
		}
		try {
			RecordFactory rf = ix.getRecordFactory();
			input = rf.createMappedRecord("input");
		} catch (Exception e) {
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		}
		return input;
	}
	
	public IRecord createNewRecordInput(String functionName, String parameter)	throws ConnectorException{
		begin();
		
		IAbstractRecord structure = createNewAbstractRecordInput(functionName, parameter);
		if (structure instanceof IRecord) {
			return (IRecord)structure;
		} else {
			String mesg=null;
			if(structure==null){
				mesg = "createNewAbstractRecordInput: No Structure Returned ";
			}else{
				mesg="createNewAbstractRecordInput returned structure of type : " +structure.getClass().getName();
			}
			throw new ConnectorException(mesg);
		}
	}
	
	public IRecordSet createNewRecordSetInput(String functionName, String parameter) throws ConnectorException{
		begin();
		
		IAbstractRecord table = createNewAbstractRecordInput(functionName, parameter);
		if (table instanceof IRecordSet) {
			return (IRecordSet)table;
		} else {
			String mesg=null;
			if(table==null){
				mesg = "createNewAbstractRecordInput: No Structure Returned ";
			}else{
				mesg="createNewAbstractRecordInput returned structure of type : " +table.getClass().getName();
			}
			throw new ConnectorException(mesg);
		}
	}
	
	/**
	 * Method for creating an input table for subsequent JCO function execution.  The returned
	 * IRecordSet has to be filled up with the JCO table/structure data and appended to the input MappedRecord, 
	 * created in createInput() method before calling execute() method.
	 * No clean-up is done before throwing an Exception, so if the execution cannot proceed an
	 * explicit call to end() is required.
	 * @param functionName JCo function name with the table/structure input data
	 * @param parameter the name of the JCO structure/table used for the input data
	 * @return IRecordSet to be filled up with the input JCO table/structure data
	 * @throws com.sap.netweaver.bc.uwl.connect.ConnectorException
	 */
	private IAbstractRecord createNewAbstractRecordInput(String functionName, String parameter)throws ConnectorException{
		
		begin();
		
		IAbstractRecord inputParameter = null;
		if (connection == null || ix == null) {
			throw  new ConnectorException("Illegal state: Connection is closed");
		}
		try {
			IFunction metaData = connection.getFunctionsMetaData().getFunction(functionName);
			// null if gunction is not found
			if (metaData == null) {
				throw new ConnectorException("Function Module " +functionName + " not found");
			}
			IStructure structure = metaData.getParameter(parameter).getStructure();
			if (structure == null) {
				throw new ConnectorException("Structure/Table " +parameter + " not found for FM " + functionName);
			}
			inputParameter =(IAbstractRecord) ix.retrieveStructureFactory().getStructure(structure);
			if(input == null)
			  createInput();
			if (input != null) {  
				input.put(parameter, inputParameter);
			}
			
			
		} catch (FunctionNotFoundException e) {
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		} catch (CapabilityNotSupportedException e) {
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		} catch (ParameterNotFoundException e) {
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		} catch (com.sapportals.connector.ConnectorException e) {
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		}
		return inputParameter;
	}
	
	public MappedRecord executeFunction(String function,boolean commit) throws com.sapportals.connector.ConnectorException {
		begin();
		
		ixspec.setPropertyValue("Name", function);
		try {
			output = (MappedRecord) ix.execute(ixspec, input);
			if (commit) {
				ixspec.setPropertyValue("Name", "ABAP4_COMMIT_WORK");
				ix.execute(ixspec, null);
			}
			if(debug)
			location.debugT(" Executed Function:"+function);
		} catch (ExecutionException e) {
			if(debug)
			location.debugT(" Failed Executed Function:"+function);
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		} catch (ConnectorException e) {
			if(debug)
			location.debugT(" Failed Executed Function:"+function);
			end();
			throw  new com.sapportals.connector.ConnectorException(e.getLocalizedMessage());
		}
		return output;
	}
	
	public MappedRecord executeFunction(String function)throws com.sapportals.connector.ConnectorException{
		return executeFunction( function, false);
	}
	
	public void end() {
		try {
			if (ix != null) {
				ix.close();
				ix = null;
			}
		} catch (Exception e) {
			location.warningT("Error in closing R3 connection to" + system);
		} finally {
			ix = null;
		}
		try {
			if (connection != null) {
				connection.close();
				location.debugT(" Closed the Connection");
				connection = null;
			}
			output = null;
			ixspec = null;
		} catch (Exception e) {
			location.warningT("Error in closing R3 connection to" + system);
		} finally {
			connection = null;
		}
	}

}
