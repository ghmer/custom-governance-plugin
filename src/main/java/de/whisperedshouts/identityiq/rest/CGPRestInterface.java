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
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import de.whisperedshouts.identityiq.util.ConfigurationValidationUtil;
import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Attributes;
import sailpoint.object.Capability;
import sailpoint.object.Configuration;
import sailpoint.object.Custom;
import sailpoint.object.Filter;
import sailpoint.object.ObjectAttribute;
import sailpoint.object.ObjectConfig;
import sailpoint.object.QueryOptions;
import sailpoint.object.Rule;
import sailpoint.object.TaskDefinition;
import sailpoint.object.Workflow;
import sailpoint.object.Workflow.Arg;
import sailpoint.object.Workflow.Step;
import sailpoint.object.Workflow.Variable;
import sailpoint.rest.plugin.BasePluginResource;
import sailpoint.rest.plugin.RequiredRight;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;
import sailpoint.tools.xml.XMLReferenceResolver;

/**
 * @author mario
 *
 */
@Path("custom-governance")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiredRight(value = CGPRestInterface.SPRIGHT_PLUGIN_ACCESS)
public class CGPRestInterface extends BasePluginResource {
  public static final String SPRIGHT_PLUGIN_ACCESS = "CGPPluginAccess";
  
  
  private static final String APPROVAL_ASSIGNMENT_RULE_ARGUMENT_NAME = "approvalAssignmentRule";
  private static final String CONFIGURATION_LCM_ACCESS_REQUEST_ATTRIBUTE_NAME = "workflowLCMAccessRequest";
  private static final String CONFIGURATION_SYSTEM_INTEGRATION_ATTRIBUTE_NAME = "customApprovalSystemIntegration";
  private static final String CUSTOM_ENTITLEMENT_CONFIGURATION_NAME = "Requestable Entitlement Configuration";
  private static final String CUSTOM_GOVERNANCE_CONFIG_NAME = "Custom Governance Model";
	private static final String CUSTOM_GOVERNANCE_RULE_NAME = "Custom Governance Model - Approval Assignment Rule";
	private static final String GOVERNANCE_OBJECT_ATTRIBUTE_NAME = "governanceApprovalLevel";
	private static final Object GROUP_REFRESH_RULE_NAME = "Requestable Entitlements - Group Refresh Rule";
  private static final Logger log	= Logger.getLogger(CGPRestInterface.class);
  
	/**
	 * return the currently defined approval levels
	 * @param context a SailPointContext
	 * @return a List of Strings representing the approval Levels
	 * @throws GeneralException upon issue
	 */
	public static List<String> getApprovalLevels(SailPointContext context) throws GeneralException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s)", "getApprovalLevels", context));
    }
    List<String> approvalLevels = new ArrayList<>();
    Custom governanceModel  = context.getObject(Custom.class, CUSTOM_GOVERNANCE_CONFIG_NAME);
    Attributes<String, Object> attributes = governanceModel.getAttributes();
    @SuppressWarnings("unchecked")
    Map<String, Object> approvalLevelsMap = (Map<String, Object>) attributes.get("approvalLevels");
    
    approvalLevels.addAll(approvalLevelsMap.keySet());
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getApprovalLevels", approvalLevels));
    }
    return approvalLevels;
  }
	
	/**
	 * return a List of rule names
	 * @param context a context to use
	 * @param type type of rules to query for. all will fetch all rules
	 * @return a list of Strings
	 * @throws GeneralException upon issue
	 */
	public static List<String> getRuleNames(SailPointContext context, String type) throws GeneralException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, type = %s)", "getRuleNames", context, type));
    }
    List<String> ruleNames = new ArrayList<>();
    QueryOptions qo = new QueryOptions();
    if(!type.equalsIgnoreCase("all")) {
      log.debug("Adding filter with value " + type);
      qo.addFilter(Filter.eq("type", type));
    }
    Iterator<Object[]> iterator = context.search(Rule.class, qo, "name");
    while(iterator.hasNext()) {
      Object[] ruleInfo = iterator.next();
      log.debug("adding Rule " + String.valueOf(ruleInfo[0]));
      ruleNames.add(String.valueOf(ruleInfo[0]));
    }
    
    Util.flushIterator(iterator);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getRuleNames", ruleNames));
    }
    return ruleNames;
  }
	
	
	
	/**
	 * return an array of application names
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Path("applicationNames")
  public Response getApplicationNames() {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "getApplicationNames"));
    }
    
    SailPointContext context = getContext();
    Response response        = null;
    try {
      List<String> applicationNames = new ArrayList<>();
      Iterator<Object[]> iterator = context.search(Application.class, new QueryOptions(), "name");
      while(iterator.hasNext()) {
        Object[] appInfo = iterator.next();
        applicationNames.add(String.valueOf(appInfo[0]));
      }
      
      Util.flushIterator(iterator);
      response = Response.ok().entity(applicationNames).build();
    } catch (GeneralException e) {
      response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getApplicationNames", response));
    }
    return response;
  }
	
	/**
	 * returns the approval Levels as an array
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Path("governanceModel/approvalLevels")
  public Response getApprovalLevels() {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "getApprovalLevels"));
    }
    SailPointContext context = getContext();
    Response response        = null;
    try {
      List<String> approvalLevels = getApprovalLevels(context);
      
      response = Response.ok().entity(approvalLevels).build();
    } catch (GeneralException e) {
      response = Response.status(Status.NOT_FOUND).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getApprovalLevels", response));
    }
    return response;
  }
	
	/**
	 * returns the entitlement configuration as a json object
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("entitlementConfiguration")
  public Response getEntitlementConfiguration() {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "getEntitlementConfiguration"));
    }
    Attributes<String, Object> result = null;
    Response response = null;
    try {
      SailPointContext context  = getContext();
      Custom custom = context.getObject(Custom.class, CUSTOM_ENTITLEMENT_CONFIGURATION_NAME);
      result = custom.getAttributes();
      response = Response.ok().entity(result.getMap()).build();
    } catch(Exception e) {
      log.error(e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getEntitlementConfiguration", response));
    }
    return response;
  }
	
	/**
	 * gets the governance model and returns it as a json object
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Path("governanceModel")
  public Response getGovernanceModel() {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "getGovernanceModel"));
    }
    SailPointContext context = getContext();
    Response response        = null;
    try {
      Custom governanceModel  = context.getObject(Custom.class, CUSTOM_GOVERNANCE_CONFIG_NAME);
      Attributes<String, Object> attributes = governanceModel.getAttributes();
      
      response = Response.ok().entity(attributes).build();
    } catch (GeneralException e) {
      response = Response.status(Status.NOT_FOUND).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getGovernanceModel", response));
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
  
  /**
	 * return an array of rule names, matching the given type
	 * @param type the rule type to query for. all acts as a special handler, returning all rules
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Path("ruleNames/{type}")
  public Response getRuleNames(@PathParam("type") String type) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(type = %s)", "getRuleNames", type));
    }
    
    SailPointContext context = getContext();
    Response response        = null;
    try {
      List<String> ruleNames = getRuleNames(context, type);
      response = Response.ok().entity(ruleNames).build();
    } catch (GeneralException e) {
      response = Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getRuleNames", response));
    }
    return response;
  }
	
	/**
	 * returns the collected information about the setup tasks
	 * @return a Response object encapsulating the json object
	 */
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
      String lcmAccessRequestWfName = (String) configuration.get(CONFIGURATION_LCM_ACCESS_REQUEST_ATTRIBUTE_NAME);
      String isSystemIntegration    = (String) configuration.get(CONFIGURATION_SYSTEM_INTEGRATION_ATTRIBUTE_NAME);
      Workflow lcmAccessRequestWf   = context.getObject(Workflow.class, lcmAccessRequestWfName);
      Map<String, Object> result    = getSetupInformation(lcmAccessRequestWf);
      result.put("workflow", lcmAccessRequestWfName);
      result.put("wfintegration", true);
      result.put("newWorkflow", String.format("%s - Custom Governance", lcmAccessRequestWfName));
      result.put("attribute", APPROVAL_ASSIGNMENT_RULE_ARGUMENT_NAME);
      result.put("rule", CUSTOM_GOVERNANCE_RULE_NAME);
      result.put("setApprovalMode", true);
      result.put("integration", (isSystemIntegration.equalsIgnoreCase("true") ? true : false));     
      result.put("aggregationRule", GROUP_REFRESH_RULE_NAME);
      result.put("tasks", getGroupAggregationTasks(context));
      result.put("setupTasks", true);
      
      response = Response.ok().entity(result).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getSetupInformation", response));
    }
    return response;
  }
	
	/**
	 * returns whether or not the given user has the SystemAdministator capability
	 * @return a Response object encapsulating the json object
	 */
	@GET
	@Path("isAdmin")
	public Response isAdmin() {
		if(log.isDebugEnabled()) {
			log.debug(String.format("ENTERING %s()", "isAdmin"));
		}
		
		Boolean isAdmin = false;
		List<Capability> userCapabilities = getLoggedInUserCapabilities();
    for(Capability cap : userCapabilities) {
      if(cap.getName().equals("SystemAdministrator")) {
        isAdmin = true;
        break;
      }
    }
		Response response = Response.ok().entity(isAdmin).build();
		
		if(log.isDebugEnabled()) {
			log.debug(String.format("LEAVING %s(return = %s)", "isAdmin", response));
		}
		return response;
	}

  /**
	 * executes the given tasks
	 * @param setupInformation Map containing information on how to setup the system
	 * @return a Response object encapsulating the json object
	 */
	@POST
  @Path("setup/performIntegration")
  public Response performIntegration(Map<String, Object> setupInformation) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(setupInformation = %s)", "performIntegration", setupInformation));
    }
    Response response            = null;
    SailPointContext context     = getContext();
    try {
      context.startTransaction();
      Configuration configuration   = context.getConfiguration();
      
      Boolean performWorkflowIntegration = Util.getBoolean(setupInformation, "wfintegration");
      Boolean performSetApprovalMode     = Util.getBoolean(setupInformation, "setApprovalMode");
      Boolean performSetupTasks          = Util.getBoolean(setupInformation, "setupTasks");
      
      Workflow workflow     = context.getObject(Workflow.class, (String)setupInformation.get("workflow"));
      if(performWorkflowIntegration) {
        Workflow newWorkflow  = (Workflow) workflow.deepCopy((XMLReferenceResolver)context);
        String newWfName      = String.valueOf(setupInformation.get("newWorkflow"));
        newWorkflow.setId(null);
        newWorkflow.setName(newWfName);
        setupWorkflow(context, newWorkflow, setupInformation);
        configuration.put(CONFIGURATION_LCM_ACCESS_REQUEST_ATTRIBUTE_NAME, newWfName);
      } else if(performSetApprovalMode) {
        setupWorkflow(context, workflow, setupInformation);
      }
      
      if(performSetupTasks) {
        setupAggregationTasks(context, setupInformation);
      }
      
      configuration.put(CONFIGURATION_SYSTEM_INTEGRATION_ATTRIBUTE_NAME, "true");
      
      context.saveObject(configuration);
      context.commitTransaction();
      response = Response.ok().entity(true).build();

    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
      try {
        context.rollbackTransaction();
      } catch (GeneralException e1) {
        log.error(e.getMessage());
      }
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "performIntegration", response));
    }
    return response;
  }
	
	/**
	 * resets the integration status, so that the system integration screen can be displayed again
	 * @return a Response object encapsulating the json object
	 */
	@GET
  @Path("setup/revertIntegrationStatus")
  public Response revertIntegrationStatus() {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s()", "revertIntegrationStatus"));
    }
    Response response            = null;
    SailPointContext context     = getContext();
    try {
      Configuration configuration   = context.getConfiguration();
      configuration.put(CONFIGURATION_SYSTEM_INTEGRATION_ATTRIBUTE_NAME, "false");
      context.saveObject(configuration);
      context.commitTransaction();
      
      response = Response.ok().entity(true).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "revertIntegrationStatus", response));
    }
    return response;
  }
	
	/**
	 * updates the Custom object using the supplied map
	 * @param entitlementConfiguration the entitlement configuration to be saved
	 * @return a Response object encapsulating the json object
	 */
	@POST
  @Path("entitlementConfiguration/update")
  public Response saveEntitlementConfiguration(Map<String, Object> entitlementConfiguration) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(entitlementConfiguration = %s)", "saveEntitlementConfiguration", entitlementConfiguration));
    }
    
    SailPointContext context = getContext();
    Response response        = null;
    try {
      Custom custom = context.getObject(Custom.class, CUSTOM_ENTITLEMENT_CONFIGURATION_NAME);
      for(Map.Entry<String, Object> entry : entitlementConfiguration.entrySet()) {
        custom.put(entry.getKey(), entry.getValue());
      }
      
      context.saveObject(custom);
      context.commitTransaction();
      
      response = Response.ok().entity(true).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "saveEntitlementConfiguration", response));
    }
    return response;
  }

  /**
	 * updates the Custom object with the given Map
	 * @param governanceModelJson the governance model to save
	 * @return a Response object encapsulating the json object
	 */
	@POST
  @Path("governanceModel/update")
  public Response saveGovernanceModel(Map<String, Object> governanceModelJson) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelJson = %s)", "saveGovernanceModel", governanceModelJson));
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

  @POST
  @Path("entitlementConfiguration/validate")
  public Response validateEntitlementConfiguration(Map<String, Object> governanceModelJson) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelJson = %s)", "validateEntitlementConfiguration", governanceModelJson));
    }
    
    SailPointContext context = getContext();
    Response response        = null;
    try {
      Map<String, Object> result = ConfigurationValidationUtil.validate(context, governanceModelJson, "entitlement");
      
      response = Response.ok().entity(result).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateEntitlementConfiguration", response));
    }
    return response;
  }
	
	@POST
  @Path("governanceModel/validate")
  public Response validateGovernanceModel(Map<String, Object> governanceModelJson) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelJson = %s)", "validateGovernanceModel", governanceModelJson));
    }
    
    SailPointContext context = getContext();
    Response response        = null;
    try {
      Map<String, Object> result = ConfigurationValidationUtil.validate(context, governanceModelJson, "approvallevel");
      
      response = Response.ok().entity(result).build();
    } catch (GeneralException e) {
      response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateGovernanceModel", response));
    }
    return response;
  }

  /**
   * return the approval levels from the given governance model
   * @param governanceModelJson the Map containing the governance model
   * @return a List containing lal approval levels of the given governance model
   */
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
	 * returns the names of defined group aggregation tasks
	 * @param context a SailPointContext to retrieve the tasks
	 * @return a List of group aggregation task names
	 * @throws GeneralException when there was an isssue retrieving the tasks
	 */
	private List<String> getGroupAggregationTasks(SailPointContext context) throws GeneralException {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s)", "getGroupAggregationTasks", context));
    }
    List<String> result   = new ArrayList<>();
    QueryOptions options  = new QueryOptions();
    
    options.addFilter(Filter.eq("type", "AccountGroupAggregation"));
    options.addFilter(Filter.eq("template", false));

    Iterator<Object[]> iterator = context.search(TaskDefinition.class, options, "name");
    while(iterator.hasNext()) {
      Object[] taskObject = iterator.next();
      result.add(String.valueOf(taskObject[0]));
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getGroupAggregationTasks", result));
    }
    return result;
  }

  /**
   * collects information from the given workflow that can be used later for setup purposes
   * @param workflow the workflow to check
   * @return a Map containing information about the given workflow
   * @throws GeneralException
   */
  private Map<String, Object> getSetupInformation(Workflow workflow) throws GeneralException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(workflow = %s)", "getSetupInformation", workflow));
    }
    Map<String, Object> result  = new HashMap<>();
    List<String> stepNames      = new ArrayList<>();
    
    Variable approvalModeVar    = workflow.getVariableDefinition("approvalMode");
    if(approvalModeVar == null || approvalModeVar.getInitializer() == null) {
      throw new GeneralException("Workflow does not contain an approval mode variable!");
    }
    String approvalMode         = approvalModeVar.getInitializer();
    
    result.put("approvalMode", approvalMode);
    
    if(workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
      throw new GeneralException("Workflow does not contain any Step");
    }
    
    for(Step step : workflow.getSteps()) {
      boolean found = false;
      List<Arg> argumentList = step.getArgs();
      if(argumentList != null && !argumentList.isEmpty()) {
        for(Arg argument : step.getArgs()) {
          if(argument.getName().equals(APPROVAL_ASSIGNMENT_RULE_ARGUMENT_NAME)) {
            stepNames.add(step.getName());
            found = true;
            break;
          }
        }
        if(!found) {
          Workflow workflowRef = step.getWorkflow();
          if(workflowRef.getName().contains("Provisioning Approval Subprocess") ||
             workflowRef.getName().contains("Approve and Provision Subprocess")) {
            stepNames.add(step.getName());
          }
        }   
      }
    }
    
    if(stepNames.isEmpty()) {
      throw new GeneralException("Workflow did not contain any modifiable Steps");
    }
    
    result.put("steps", stepNames);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getSetupInformation", result));
    }
    
    return result;
  }
	
	@SuppressWarnings("unchecked")
  private void setupAggregationTasks(SailPointContext context, Map<String, Object> setupInformation) throws GeneralException {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, setupInformation = %s)", "setupAggregationTasks", context, setupInformation));
    }
	  
	  String ruleName        = String.valueOf(setupInformation.get("aggregationRule"));
	  List<String> taskNames = (List<String>) setupInformation.get("tasks");
	  
	  for(String taskName : taskNames) {
	    TaskDefinition task = context.getObject(TaskDefinition.class, taskName);
	    task.setArgument("accountGroupRefreshRule", ruleName);
	    
	    context.saveObject(task);
	  }
	  
	  if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "setupAggregationTasks", "null"));
    }
  }
	
	/**
	 * performs setup tasks on the given workflow
	 * @param context 
	 * @param workflow the Workflow to setup
	 * @param setupInformation the Map containing the setup information
	 * @throws GeneralException when there was an issue
	 */
	private void setupWorkflow(SailPointContext context, Workflow workflow, Map<String, Object> setupInformation) throws GeneralException {
	  if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, workflow = %s, setupInformation = %s)", "setupWorkflow", context, workflow, setupInformation));
    }
	  
	  String ruleName      = (String) setupInformation.get("rule");
	  String approvalMode  = (String) setupInformation.get("approvalMode");
	  
	  Boolean performWorkflowIntegration = Util.getBoolean(setupInformation, "wfintegration");
    Boolean performSetApprovalMode     = Util.getBoolean(setupInformation, "setApprovalMode");
	  
    if(performWorkflowIntegration) {
      @SuppressWarnings("unchecked")
      List<String> steps   = (List<String>) setupInformation.get("steps");
      for(String stepName : steps) {
        Step step = workflow.getStep(stepName);
        Boolean found = false;
        for(Arg argument : step.getArgs()) {
          if(argument.getName().equals(APPROVAL_ASSIGNMENT_RULE_ARGUMENT_NAME)) {
            argument.setValue(ruleName);
            found = true;
            break;
          }
        }
        if(!found) {
          Arg argument = new Arg();
          step.getArgs().add(argument);
        }     
      }
    }
	  
	  if(performSetApprovalMode) {
	    workflow.getVariableDefinition("approvalMode").setInitializer(approvalMode);
	  }

	  context.saveObject(workflow);
	  
	  if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "setupWorkflow", null));
    }
	}
	
	/**
   * updates the Custom object with the information in the given governance model
   * @param context a SailPointContext
   * @param governanceModelJson a Map containing a governance model
   * @throws GeneralException when there was an issue updating the model
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

	/**
	 * updates a ObjectConfig object
	 * @param context a SailPointObject
	 * @param objectConfigName the ObjectConfig name to be updates
	 * @param attributeName the attribute to update
	 * @param allowedValues the allowed values to set
	 * @throws GeneralException when there was an issue with the SailPointObjects
	 */
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

}
