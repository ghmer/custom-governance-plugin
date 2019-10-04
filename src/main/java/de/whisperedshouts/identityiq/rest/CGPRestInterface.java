/**
 * 
 */
package de.whisperedshouts.identityiq.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.object.Attributes;
import sailpoint.object.Custom;
import sailpoint.object.ObjectAttribute;
import sailpoint.object.ObjectConfig;
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
	public static final String GOVERNANCE_OBJECT_ATTRIBUTE_NAME = "governanceApprovalLevel";
	
	@GET
	@Path("getGovernanceModel")
	public Response getGovernanceModel() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "getGovernanceModel"));
		}
		SailPointContext context = getContext();
		Response response 		   = null;
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
	
	@GET
	@Path("getRuleNames")
	public Response getRuleNames() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "getRuleNames"));
		}
		
		SailPointContext context = getContext();
		Response response 		   = null;
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
	
	@GET
	@Path("isAdmin")
	public Response isAdmin() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "isAdmin"));
		}
		
		Response response = Response.ok().entity(false).build();
		
		if(log.isDebugEnabled()) {
			log.debug(String.format("LEAVING %s(return = %s)", "isAdmin", response));
		}
		return response;
	}
	
	@POST
	@Path("saveGovernanceModel")
	public Response saveGovernanceModel(Map<String, Object> governanceModelJson) {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "saveGovernanceModel"));
    }
	  
	  SailPointContext context = getContext();
    Response response        = null;
    try {
      List<Object> allowedValues = getApprovalLevels(governanceModelJson);
      
      updateGovernanceModel(context, governanceModelJson);
      updateObjectConfig(context, "ManagedAttribute", GOVERNANCE_OBJECT_ATTRIBUTE_NAME, allowedValues);
      updateObjectConfig(context, "Bundle", GOVERNANCE_OBJECT_ATTRIBUTE_NAME, allowedValues);
      
      response = Response.ok().entity(true).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "saveGovernanceModel", response));
    }
    return response;
	}

  @SuppressWarnings("unchecked")
  private List<Object> getApprovalLevels(Map<String, Object> governanceModelJson) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelJson  = %s)", "getApprovalLevels", governanceModelJson));
    }
    
    List<Object> result = new ArrayList<>();
    Map<Object, Object> approvalLevelMap = (Map<Object, Object>) governanceModelJson.get("approvalLevels");
    for(Object approvalLevel : approvalLevelMap.keySet()) {
      result.add(approvalLevel);
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getApprovalLevels", result));
    }
    return result;
  }

  /**
   * @param context
   * @param governanceModelJson
   * @throws GeneralException
   */
  private void updateGovernanceModel(SailPointContext context, Map<String, Object> governanceModelJson)
      throws GeneralException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, governanceModelJson = %s)", "updateGovernanceModel", context, governanceModelJson));
    }
    
    Custom governanceModel  = context.getObject(Custom.class, CUSTOM_GOVERNANCE_CONFIG_NAME);
    for(Map.Entry<String, Object> entry : governanceModelJson.entrySet()) {
      governanceModel.put(entry.getKey(), entry.getValue());
    }

    context.saveObject(governanceModel);
    context.commitTransaction();
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "updateGovernanceModel", null));
    }
  }
	
	private void updateObjectConfig(SailPointContext context, String objectConfigName, String attributeName, List<Object> allowedValues) throws GeneralException {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, objectConfigName = %s, attributeName = %s, allowedValues = %s)", "updateObjectConfig", context, objectConfigName, attributeName, allowedValues));
    }
	  
	  ObjectConfig objectConfig = context.getObject(ObjectConfig.class, objectConfigName);
	  ObjectAttribute objectAttribute = objectConfig.getObjectAttribute(attributeName);
	  objectAttribute.setAllowedValues(allowedValues);
	  
	  context.saveObject(objectConfig);
	  context.commitTransaction();
	  
	  if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "updateObjectConfig", null));
    }
	}
	
	/* (non-Javadoc)
	 * @see sailpoint.rest.plugin.BasePluginResource#getPluginName()
	 */
	@Override
	public String getPluginName() {
		return "custom_governance_plugin";
	}

}
