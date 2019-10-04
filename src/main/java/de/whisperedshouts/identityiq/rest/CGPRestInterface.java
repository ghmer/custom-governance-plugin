/**
 * 
 */
package de.whisperedshouts.identityiq.rest;

import java.util.ArrayList;
import java.util.HashMap;
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
import sailpoint.object.Configuration;
import sailpoint.object.Custom;
import sailpoint.object.ObjectAttribute;
import sailpoint.object.ObjectConfig;
import sailpoint.object.QueryOptions;
import sailpoint.object.Rule;
import sailpoint.object.Workflow;
import sailpoint.object.Workflow.Arg;
import sailpoint.object.Workflow.Step;
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
	@Path("governanceModel")
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
	@Path("ruleNames")
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
			response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
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
	@Path("governanceModel/update")
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
	
	@GET
	@Path("setup/information")
	public Response getSetupInformation() {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "getSetupInformation"));
    }
	  Response response            = null;
	  SailPointContext context     = getContext();
	  try {
      Configuration configuration   = context.getConfiguration();
      String lcmAccessRequestWfName = (String) configuration.get("workflowLCMAccessRequest");
      Workflow lcmAccessRequestWf   = context.getObject(Workflow.class, lcmAccessRequestWfName);
      Map<String, Object> result    = getSetupInformation(lcmAccessRequestWf);
      result.put("workflow", lcmAccessRequestWfName);
      
      response = Response.ok().entity(result).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
	  
	  if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getSetupInformation", response));
    }
	  return response;
	}

  private Map<String, Object> getSetupInformation(Workflow workflow) throws GeneralException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(workflow = %s)", "getSetupInformation", workflow));
    }
    List<String> stepNames = new ArrayList<>();
    if(workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
      throw new GeneralException("Workflow does not contain any Step");
    }
    
    for(Step step : workflow.getSteps()) {
      boolean found = false;
      List<Arg> argumentList = step.getArgs();
      if(argumentList != null && !argumentList.isEmpty()) {
        for(Arg argument : step.getArgs()) {
          if(argument.getName().equals("approvalAssignmentRule")) {
            stepNames.add(step.getName());
            found = true;
            break;
          }
        }
        if(!found) {
          Workflow workflowRef = step.getWorkflow();
          if(workflowRef.getName().equals("Provisioning Approval Subprocess") ||
             workflowRef.getName().equals("Approve and Provision Subprocess")) {
            stepNames.add(step.getName());
          }
        }   
      }
    }
    
    if(stepNames.isEmpty()) {
      throw new GeneralException("Workflow did not contain any modifiable Steps");
    }
    
    Map<String, Object> result = new HashMap<>();
    result.put("steps", stepNames);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getSetupInformation", result));
    }
    
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<Object> getApprovalLevels(Map<String, Object> governanceModelJson) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelJson = %s)", "getApprovalLevels", governanceModelJson));
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
	
	private void setupWorkflow(Workflow workflow, String ruleName) throws GeneralException {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(workflow = %s, ruleName = %s)", "setupWorkflow", workflow, ruleName));
    }
	  int modifyCount = 0;
	  
	  if(workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
	    throw new GeneralException("Workflow does not contain any Step");
	  }
	  
	  for(Step step : workflow.getSteps()) {
	    boolean found = false;
	    List<Arg> argumentList = step.getArgs();
	    if(argumentList != null && !argumentList.isEmpty()) {
	      for(Arg argument : step.getArgs()) {
	        if(argument.getName().equals("approvalAssignmentRule")) {
	          argument.setValue(ruleName);
	          found = true;
	          modifyCount++;
	          break;
	        }
	      }
	      if(!found) {
	        Workflow workflowRef = step.getWorkflow();
	        if(workflowRef.getName().equals("Provisioning Approval Subprocess") ||
	           workflowRef.getName().equals("Approve and Provision Subprocess")) {
	          Arg argument = new Arg();
	          step.getArgs().add(argument);
	          modifyCount++;
	        }
	      }   
	    }
	  }
	  
	  if(modifyCount == 0) {
	    throw new GeneralException("Workflow did not contain any modifiable Steps");
	  }
	  if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "setupWorkflow", null));
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
