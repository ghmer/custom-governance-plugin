/**
 * 
 */
package de.whisperedshouts.identityiq.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Custom;
import sailpoint.object.QueryOptions;
import sailpoint.object.Rule;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.rest.plugin.RequiredRight;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * @author mario
 *
 */
@Path("custom-governance")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredRight(value = CGPRestInterface.SPRIGHT_PLUGIN_ACCESS)
public class CGPRestInterface extends BasePluginResource {

	private static final Logger log	= Logger.getLogger(CGPRestInterface.class);
	
	public static final String SPRIGHT_PLUGIN_ACCESS = "CGPPluginAccess";
	public static final String CUSTOM_GOVERNANCE_CONFIG_NAME = "Custom Governance Model";
	
	@Path("/getGovernanceModel")
	public Response getGovernanceModel() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "getGovernanceModel"));
		}
		SailPointContext context = getContext();
		Response response 		 = null;
		try {
			Custom governanceModel  = context.getObject(Custom.class, CUSTOM_GOVERNANCE_CONFIG_NAME);
			Attributes<String, Object> attributes	= governanceModel.getAttributes();
			
			response = Response.ok().entity(attributes).build();
		} catch (GeneralException e) {
			response = Response.status(Status.NOT_FOUND).build();
		} 
		
		if(log.isDebugEnabled()) {
			log.debug(String.format("LEAVING %s(return = %s)", "getGovernanceModel", response));
		}
		return response;
	}
	
	@Path("/getRuleNames")
	public Response getRuleNames() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "getRuleNames"));
		}
		SailPointContext context = getContext();
		Response response 		 = null;
		try {
			List<String> ruleNames = new ArrayList<>();
			Iterator<Object[]> iterator = context.search(Rule.class, new QueryOptions(), "name");
			while(iterator.hasNext()) {
				Object[] ruleInfo = iterator.next();
				ruleNames.add(String.valueOf(ruleInfo[0]));
			}
			
			Util.flushIterator(iterator);
			response = Response.ok().entity(ruleNames).build();
		} catch (GeneralException e) {
			response = Response.status(Status.NOT_FOUND).build();
		} 
		
		if(log.isDebugEnabled()) {
			log.debug(String.format("LEAVING %s(return = %s)", "getRuleNames", response));
		}
		return response;
	}
	
	/* (non-Javadoc)
	 * @see sailpoint.rest.plugin.BasePluginResource#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "custom_governance_plugin";
	}

}
