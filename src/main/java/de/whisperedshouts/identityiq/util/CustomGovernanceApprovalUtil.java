/**
 * 
 */
package de.whisperedshouts.identityiq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import sailpoint.api.ManagedAttributer;
import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.ApprovalItem;
import sailpoint.object.ApprovalSet;
import sailpoint.object.Attributes;
import sailpoint.object.Bundle;
import sailpoint.object.Custom;
import sailpoint.object.Identity;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.Rule;
import sailpoint.object.Workflow;
import sailpoint.object.WorkItem.State;
import sailpoint.object.Workflow.Approval;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * @author mario.ragucci
 *
 */
public class CustomGovernanceApprovalUtil {

  SailPointContext context = null;
  private static Logger log = Logger.getLogger(CustomGovernanceApprovalUtil.class);

  /**
   * Constructor. Not much about it
   * 
   * @param context
   *          a SailPointContext
   */
  public CustomGovernanceApprovalUtil(SailPointContext context) {
    this.context = context;
  }

  /**
   * Processes the approvalSet by checking against the securityType and the
   * Custom Governance Configuration
   * 
   * @param approvalSet
   *          the ApprovalSet being processed
   * @param approvals
   *          the List of already created Approvals
   * @param workflow
   *          the workflow invoking the approval assignment rule
   * @return a List of approvals
   * @throws GeneralException when checks are failing
   */
  @SuppressWarnings("unchecked")
  public List<Approval> processApprovalSet(ApprovalSet approvalSet, List<Approval> approvals, Workflow workflow)
      throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvalSet = %s, approvals = %s, workflow = %s)", "processApprovalSet",
          approvalSet, approvals, workflow));
    }
    Map<String, Object> approvalVariables = generateBaseApprovalVariablesMap(workflow);
    // Initialize finalApprovals to an empty list to hold any additional
    // approvals needed
    List<Approval> finalApprovals = new ArrayList<Approval>();

    if (null == approvals) {
      log.warn("No default approvals set!");
    }

    if (null == approvalSet) {
      log.error("ApprovalSet not set!");
      throw new GeneralException("ApprovalSet not set");
    }

    if (log.isTraceEnabled()) {
      log.trace("Approval Set:\n" + approvalSet.toXml());
    }

    Attributes<String, Object> governanceConfiguration = getGovernanceLevels(
        (String) approvalVariables.get("governanceModelName"));

    for (ApprovalItem approvalItem : approvalSet.getItems()) {
      String requestType = determineRequestType(approvalItem);
      Map<String, Object> requestObject = new HashMap<>();
      switch (requestType) {
        case "Role":
          requestObject = getRequestObjectInfoForRoles(approvalItem);
          break;
        case "Entitlement":
          requestObject = getRequestObjectInfoForEntitlements(approvalItem);
          break;
        case "Account":
          break;
        default:
          throw new GeneralException("requestType could not be determined");
      }

      if (requestType.equals("Account")) {
        // returning default approvals
        if (log.isDebugEnabled()) {
          log.debug(String.format("LEAVING %s(return = %s)", "processApprovalSet", approvals));
        }
        return approvals;
      }

      approvalVariables.put("owner", requestObject.get("owner"));
      approvalVariables.put("attributes", requestObject.get("attributes"));

      Map<String, Object> governanceLevels = (Map<String, Object>) governanceConfiguration
          .get(approvalVariables.get("approvalLevelKey"));

      determineApprovalAndNotificationSchemes(governanceLevels, approvalVariables,
          (Attributes<String, Object>) requestObject.get("attributes"));

      List<String> approverList = (List<String>) approvalVariables.get("approverList");

      if (Util.isEmpty(approverList)) {
        handleEmptyApproverList(approvals, finalApprovals, approvalItem,
            (String) approvalVariables.get("identityName"));
      } else {
        Map<String, Object> governanceApproverRules = (Map<String, Object>) governanceConfiguration
            .get(approvalVariables.get("approvalRuleKey"));
        handleApproverList(approvalVariables, finalApprovals, approvalItem, approverList, governanceApproverRules);

      }
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "processApprovalSet", finalApprovals));
    }
    return finalApprovals;
  }

  /**
   * adds a new Approval to the list of approvals
   * 
   * @param approvalVariables
   *          a Map holding certain workflow variables
   * @param finalApprovals
   *          the List of Approvals being generated
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param approvalOwner
   *          the approval owner to be set
   * @param descriptionPrefix
   *          the prefix on the description of the workitem
   */
  private void addNewApproval(Map<String, Object> approvalVariables, List<Approval> finalApprovals,
      ApprovalItem approvalItem, String approvalOwner, String descriptionPrefix) {
    if (log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(approvalVariables = %s,finalApprovals = %s, approvalItem = %s, "
              + "approvalOwner = %s, descriptionPrefix = %s)",
          "addNewApproval", approvalVariables, finalApprovals, approvalItem, approvalOwner, descriptionPrefix));
    }

    if (approvalOwner.equalsIgnoreCase("Auto Approved")) {
      finalApprovals.add(createApprovedApproval(approvalItem, (String) approvalVariables.get("identityName"),
          approvalOwner, generateApprovedApprovalDescription(descriptionPrefix,
              (String) approvalVariables.get("identityDisplayName"))));
    } else {
      finalApprovals.add(createApproval(approvalItem, (String) approvalVariables.get("identityName"), approvalOwner,
          generateApprovalDescription(descriptionPrefix, (String) approvalVariables.get("identityDisplayName"))));
    }
    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "addNewApproval", "null"));
    }
  }

  /**
   * Adds to an existing approval
   * 
   * @param approvalVariables
   *          a Map holding certain workflow variables
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param approvalObject
   *          an Approval Object
   * @param descriptionPrefix
   *          the prefix on the description of the workitem
   */
  private void addToExistingApproval(Map<String, Object> approvalVariables, ApprovalItem approvalItem,
      Approval approvalObject, String descriptionPrefix) {
    if (log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(approvalVariables = %s, approvalItem = %s, " + "approvalObject = %s, descriptionPrefix = %s)",
          "addToExistingApproval", approvalVariables, approvalItem, approvalObject, descriptionPrefix));
    }

    // Add approval item to existing approval
    ApprovalSet existingApprovalSet = approvalObject.getApprovalSet();
    if (null != existingApprovalSet && existingApprovalSet.find(approvalItem) == null) {
      existingApprovalSet.add(approvalItem);
      approvalObject.setApprovalSet(existingApprovalSet);
      if (!approvalObject.getDescription().startsWith(descriptionPrefix)) {
        approvalObject.setDescription(generateApprovalDescription((String) approvalVariables.get("defaultDescPrefix"),
            (String) approvalVariables.get("identityDisplayName")));
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "addToExistingApproval", null));
    }
  }

  // Helper method to create a new Workflow.Approval
  /**
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param identityName
   *          the identityName of the requestee
   * @param owner
   *          the approval owner to be set
   * @param description
   *          the description to be used
   * @return an Approval
   */
  private Approval createApproval(ApprovalItem approvalItem, String identityName, String owner, String description) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvalItem = %s, identityName = %s, owner = %s, description = %s)",
          "createApproval", approvalItem, identityName, owner, description));
    }

    Approval approval = new Approval();
    approval.setDescription(description);
    approval.setOwner(owner);
    ApprovalSet apprSet = new ApprovalSet();
    apprSet.add(approvalItem);
    approval.setApprovalSet(apprSet);
    approval.addArg("workItemTargetClass", "sailpoint.object.Identity");
    approval.addArg("workItemTargetName", identityName);
    approval.addArg("workItemNotificationTemplate", "LCM Identity Update Approval");

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "createApproval", approval));
    }
    return approval;
  }

  // Helper method to create a new Auto-Approved Workflow.Approval
  /**
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param identityName
   *          the identityName of the requestee
   * @param owner
   *          the approval owner to be set
   * @param description
   *          the description to be used
   * @return an Approval
   */
  private Approval createApprovedApproval(ApprovalItem approvalItem, String identityName, String owner,
      String description) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvalItem = %s, identityName = %s, owner = %s, description = %s)",
          "createApprovedApproval", approvalItem, identityName, owner, description));
    }

    Approval approval = new Approval();
    approval.setState(State.Finished);
    approval.setComplete(true);
    approval.setDescription(description);
    if (owner.equalsIgnoreCase("Auto Approved")) {
      approval.setOwner("spadmin");
    } else {
      approval.setOwner(owner);
    }

    ApprovalSet apprSet = new ApprovalSet();
    approvalItem.approve();
    approvalItem.setState(State.Finished);
    apprSet.add(approvalItem);
    approval.setApprovalSet(apprSet);
    approval.addArg("workItemTargetClass", "sailpoint.object.Identity");
    approval.addArg("workItemTargetName", identityName);
    approval.addArg("workItemNotificationTemplate", "LCM Identity Update Approval");

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "createApprovedApproval", approval));
    }
    return approval;
  }

  /**
   * determines which approval and notification scheme to use for the given
   * requested item
   * 
   * @param governanceLevels
   *          the governance level configuration
   * @param approvalVariables
   *          a Map holding certain workflow variables
   * @param requestObjectAttributes
   *          information about the object being requested
   */
  @SuppressWarnings("unchecked")
  private void determineApprovalAndNotificationSchemes(Map<String, Object> governanceLevels,
      Map<String, Object> approvalVariables, Attributes<String, Object> requestObjectAttributes) {
    if (log.isDebugEnabled()) {
      log.debug(
          String.format("ENTERING %s(governanceLevels = %s, approvalVariables = %s, requestObjectAttributes = %s)",
              "determineApprovalAndNotificationSchemes", governanceLevels, approvalVariables, requestObjectAttributes));
    }

    // Determine the approval and notification schemes based on governance level
    List<String> approverList = new ArrayList<String>();
    List<String> customNotificationScheme = new ArrayList<String>();
    String approvalLevel = requestObjectAttributes.getString((String) approvalVariables.get("governanceAttr"));

    log.trace("specified level: " + approvalLevel);
    if (!Util.isNullOrEmpty(approvalLevel) && governanceLevels.containsKey(approvalLevel)) {
      if (log.isDebugEnabled()) {
        log.debug("Using approval and notification schemes for Governance " + Util.splitCamelCase(approvalLevel));
      }
      Map<String, Object> governanceLevel = (HashMap<String, Object>) governanceLevels.get(approvalLevel);
      approverList = (List<String>) governanceLevel.get(approvalVariables.get("approvalKey"));
      customNotificationScheme = (List<String>) governanceLevel.get(approvalVariables.get("notificationKey"));
      Set<String> notificationScheme = (Set<String>) approvalVariables.get("notificationScheme");

      if (Util.isEmpty(customNotificationScheme)) {
        log.warn("No notification scheme set for specified governance level");
        // Merge custom notifications with the existing notificaiton scheme
      } else {
        notificationScheme.clear();
        notificationScheme.addAll(new HashSet<String>(customNotificationScheme));
        log.debug("Notification scheme updated");
      }

      approvalVariables.put("approverList", approverList);
    }
    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "determineApprovalAndNotificationSchemes", "null"));
    }
  }

  /**
   * checks the request type based on the Approval Item
   * 
   * @param item
   *          the ApprovalItem to be checked
   * @return the type being requested
   */
  private String determineRequestType(ApprovalItem item) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(item = %s)", "determineRequestType", item));
    }
    String operation = (String) item.getAttribute("operation");
    String requestType = null;
    if (operation != null) {
      if (operation.indexOf("Role") > -1) {
        requestType = "Role";
      } else if (operation.indexOf("Entitlement") > -1) {
        requestType = "Entitlement";
      } else {
        requestType = "Account";
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "determineRequestType", requestType));
    }
    return requestType;
  }

  /**
   * Helper method to generate an approval work item description based on an
   * optional prefix
   * 
   * @param prefix
   *          the prefix to be used
   * @param identityName
   *          the identityName of the requestee
   * @return the generated approval description
   */
  private String generateApprovalDescription(String prefix, String identityName) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(prefix = %s, identityName = %s)", "generateApprovalDescription", prefix,
          identityName));
    }
    String desc = "Approval - Account Changes for User: " + identityName;
    String result = (Util.isNullOrEmpty(prefix)) ? desc : prefix + " " + desc;

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "generateApprovalDescription", result));
    }
    return result;
  }

  /**
   * Helper method to generate an auto-approval work item description based on
   * an optional prefix
   * 
   * @param prefix
   *          the prefix to be used
   * @param identityName
   *          the identityName of the requestee
   * @return the generated approval description
   */
  private String generateApprovedApprovalDescription(String prefix, String identityName) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(prefix = %s, identityName = %s)", "generateApprovedApprovalDescription",
          prefix, identityName));
    }
    String desc = "Auto Approved - Account Changes for User: " + identityName;
    String result = (Util.isNullOrEmpty(prefix)) ? desc : prefix + " " + desc;

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "generateApprovedApprovalDescription", result));
    }
    return result;
  }

  /**
   * Helper method to generate a configuration Map.
   * 
   * @param workflow
   *          the workflow invoking the approval assignment rule
   * @return a Map containing basis approval variables
   * @throws GeneralException
   */
  private Map<String, Object> generateBaseApprovalVariablesMap(Workflow workflow) throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(workflow = %s)", "generateBaseApprovalVariablesMap", workflow));
    }
    Map<String, Object> result = new HashMap<String, Object>();

    if (null == workflow) {
      log.error("Worflow not set!");
      throw new GeneralException("Workflow not set!");
    }

    if (Util.isNullOrEmpty((String) workflow.get("identityName"))) {
      log.error("Workflow variable identityName not set!");
      throw new GeneralException("Workflow variable identityName not set!");
    }

    // Should not have to change the following constants
    result.put("governanceModelName", "Custom Governance Model");
    result.put("delimiter", ":");
    result.put("defaultDescPrefix", "Access Request");
    result.put("governanceAttr", "governanceApprovalLevel");
    result.put("approvalKey", "approval");
    result.put("notificationKey", "notification");
    result.put("approvalLevelKey", "approvalLevels");
    result.put("approvalRuleKey", "approverLookupRules");
    result.put("identityName", workflow.get("identityName"));
    result.put("identityDisplayName", Util.isNullOrEmpty((String) workflow.get("identityDisplayName"))
        ? result.get("identityName") : workflow.get("identityDisplayName"));
    result.put("launcher", workflow.get("launcher"));
    result.put("securityOfficer", workflow.get("securityOfficerName"));
    result.put("notificationScheme", Util.csvToSet((String) workflow.get("notificationScheme"), true));
    result.put("fallbackApprover", (String) workflow.get("fallbackApprover"));

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "generateBaseApprovalVariablesMap", result));
    }
    return result;
  }

  /**
   * gets the Attributes of the Custom Object containing the governance mode
   * 
   * @param governanceModelName
   *          the name of the Custom Object containing the governance model
   * @return the Attributes of the governance model
   * @throws GeneralException
   */
  private Attributes<String, Object> getGovernanceLevels(String governanceModelName) throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(governanceModelName = %s)", "getGovernanceLevels", governanceModelName));
    }
    Custom governanceModel = null;
    Attributes<String, Object> governanceLevels = null;
    try {
      governanceModel = context.getObjectByName(Custom.class, governanceModelName);
    } catch (GeneralException ge) {
      log.error("Exception caught while retriving SailPoint Custom object with the name: " + governanceModelName);
    }

    if (null == governanceModel) {
      log.error("Could not get governance model!");
      throw new GeneralException("Could not get governance Model");
    }

    if (log.isTraceEnabled()) {
      log.trace("Governance Model:\n" + governanceModel.toXml());
    }
    governanceLevels = governanceModel.getAttributes();
    if (null == governanceLevels) {
      log.error("Could not get attributes from custom governance model!");
      throw new GeneralException("Could not get attributes from custom governance model");
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getGovernanceLevels", governanceLevels));
    }
    return governanceLevels;
  }

  /**
   * returns a Map containing information about the requested entitlement
   * 
   * @param approvalItem
   *          the ApprovalItem being processed
   * @return a Map containing information about the requested entitlement
   * @throws GeneralException
   */
  private Map<String, Object> getRequestObjectInfoForEntitlements(ApprovalItem approvalItem) throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvalItem = %s)", "getRequestObjectInfoForEntitlements", approvalItem));
    }
    Map<String, Object> result = new HashMap<>();
    List<String> valueList = approvalItem.getValueList();
    String assignmentValue = null;
    if (Util.isEmpty(valueList)) {
      log.error("Could not get value of ApprovalItem as a List!");
    } else {
      assignmentValue = valueList.get(0);
    }

    Attributes<String, Object> requestObjectAttributes = null;
    Identity requestObjectOwner = null;

    String assignmentName = approvalItem.getName();
    Application app = null;
    try {
      app = context.getObjectByName(Application.class, approvalItem.getApplication());
    } catch (GeneralException ge) {
      log.error("Exception caught while retriving SailPoint Application object!");
    }

    if (app != null) {
      ManagedAttribute entitlement = null;
      try {
        entitlement = ManagedAttributer.get(context, app, assignmentName, assignmentValue);
      } catch (GeneralException ge) {
        log.error("Exception caught while retriving SailPoint ManagedAttribute object!");
      }

      if (null != entitlement) {
        requestObjectAttributes = entitlement.getAttributes();
        requestObjectOwner = entitlement.getOwner();
      }

      if (requestObjectAttributes == null) {
        log.error("Could not get role attributes!");
        throw new GeneralException("Could not get managed object attributes");
      }
    }

    result.put("attributes", requestObjectAttributes);
    result.put("owner", requestObjectOwner);

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getRequestObjectInfoForEntitlements", result));
    }
    return result;
  }

  /**
   * returns a Map containing information about the requested role
   * 
   * @param approvalItem
   *          the ApprovalItem being processed
   * @return a Map containing information about the requested role
   * @throws GeneralException
   */
  private Map<String, Object> getRequestObjectInfoForRoles(ApprovalItem approvalItem) throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvalItem = %s)", "getRequestObjectInfoForRoles", approvalItem));
    }
    Map<String, Object> result = new HashMap<>();
    List<String> valueList = approvalItem.getValueList();
    String assignmentValue = null;
    if (Util.isEmpty(valueList)) {
      log.error("Could not get value of ApprovalItem as a List!");
    } else {
      assignmentValue = valueList.get(0);
    }

    Attributes<String, Object> requestObjectAttributes = null;
    Identity requestObjectOwner = null;

    Bundle role = null;
    try {
      role = context.getObjectByName(Bundle.class, assignmentValue);
    } catch (GeneralException ge) {
      log.error("Exception caught while retriving SailPoint Bundle object with the name: " + assignmentValue);
    }

    if (null != role) {
      requestObjectAttributes = role.getAttributes();
      requestObjectOwner = role.getOwner();
    }

    if (requestObjectAttributes == null) {
      log.error("Could not get role attributes!");
      throw new GeneralException("Could not get role attributes");
    }

    result.put("attributes", requestObjectAttributes);
    result.put("owner", requestObjectOwner);

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "getRequestObjectInfoForRoles", result));
    }
    return result;
  }

  /**
   * @param approvalVariables
   *          a Map holding certain workflow variables
   * @param finalApprovals
   *          the List of Approvals being generated
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param approverList
   *          a list containing approvers
   * @param governanceApproverRules
   *          the governance approver rule configuration
   * @throws GeneralException
   */
  private void handleApproverList(Map<String, Object> approvalVariables, List<Approval> finalApprovals,
      ApprovalItem approvalItem, List<String> approverList, Map<String, Object> governanceApproverRules)
      throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(approvalVariables = %s, finalApprovals = %s, approvalItem = %s, approverList = %s, governanceApproverRules = %s)",
          "handleApproverList", approvalVariables, finalApprovals, approvalItem, approverList,
          governanceApproverRules));
    }

    for (String approver : approverList) {
      String approvalOwner = null;
      String descPrefix = null;

      descPrefix = Util.splitCamelCase(approver);
      approvalOwner = runRule(approvalVariables, governanceApproverRules, approvalItem, approver);

      if (Util.isNullOrEmpty(approvalOwner) || Util.nullSafeEq(approvalOwner, approvalVariables.get("launcher"))) {
        log.warn("Skipping approval type " + approver);
      } else {
        Approval approvalObject = null;
        for (Approval existingApproval : finalApprovals) {
          if (approvalOwner.equals(existingApproval.getOwner())) {
            approvalObject = existingApproval;
            break;
          }
        }

        if (null == approvalObject) {
          addNewApproval(approvalVariables, finalApprovals, approvalItem, approvalOwner, descPrefix);
        } else {
          addToExistingApproval(approvalVariables, approvalItem, approvalObject, descPrefix);
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "handleApproverList", "null"));
    }
  }

  /**
   * @param finalApprovals
   *          the List of Approvals being generated
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param identityName
   *          the identityName of the requestee
   * @param approval
   *          the approval being processed
   */
  private void handleEmptyApprover(List<Approval> finalApprovals, ApprovalItem approvalItem, String identityName,
      Approval approval) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(finalApprovals = %s, approvalItem = %s, identityName = %s, approval = %s)",
          "handleEmptyApprover", finalApprovals, approvalItem, identityName, approval));
    }
    ApprovalSet approvalSet = approval.getApprovalSet();
    String approvalOwner = approval.getOwner();
    if (approvalSet != null && approvalSet.find(approvalItem) != null && Util.isNotNullOrEmpty(approvalOwner)) {
      // remove quote characters from owner string
      approvalOwner = approvalOwner.replace("\"", "");
      if (log.isDebugEnabled()) {
        log.debug("Searching existing approvals for " + approvalOwner);
      }
      boolean newApproval = true;
      for (Approval existingApproval : finalApprovals) {
        if (approvalOwner.equals(existingApproval.getOwner())) {
          log.debug("Match found, adding default approval item to existing approval...");
          ApprovalSet existingApprovalSet = existingApproval.getApprovalSet();
          if (null != existingApprovalSet && existingApprovalSet.find(approvalItem) == null) {
            existingApprovalSet.add(approvalItem);
            existingApproval.setApprovalSet(existingApprovalSet);
          }
          newApproval = false;
          break;
        }
      }
      if (newApproval) {
        log.debug("No match found, creating new default approval...");
        finalApprovals.add(createApproval(approvalItem, identityName, approvalOwner, approval.getDescription()));
      }
    }
    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "handleEmptyApprover", "null"));
    }
  }

  /**
   * @param approvals
   *          the List of already created Approvals
   * @param finalApprovals
   *          the List of Approvals being generated
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param identityName
   *          the identityName of the requestee
   */
  private void handleEmptyApproverList(List<Approval> approvals, List<Approval> finalApprovals,
      ApprovalItem approvalItem, String identityName) {
    if (log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(approvals = %s, finalApprovals = %s, approvalItem = %s, identityName = %s)",
          "handleEmptyApproverList", approvals, finalApprovals, approvalItem, identityName));
    }
    log.debug("No additional approvals necessary.");
    // Add back default approvals for the approval item
    for (Approval approval : approvals) {
      handleEmptyApprover(finalApprovals, approvalItem, identityName, approval);
    }
    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "handleEmptyApproverList", "null"));
    }
  }

  /**
   * @param approvalVariables
   *          a Map holding certain workflow variables
   * @param governanceApproverRules
   *          the governance approver rule configuration
   * @param approvalItem
   *          the ApprovalItem being processed
   * @param approverName
   *          the identity name of the approver
   * @return
   * @throws GeneralException
   */
  private String runRule(Map<String, Object> approvalVariables, Map<String, Object> governanceApproverRules,
      ApprovalItem approvalItem, String approverName) throws GeneralException {
    if (log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(approvalVariables = %s, governanceApproverRules = %s, approvalItem = %s, approverName = %s)",
          "runRule", approvalVariables, governanceApproverRules, approvalItem, approverName));
    }
    String result = null;
    Rule ruleObject = null;
    try {
      String ruleName = (String) governanceApproverRules.get(approverName);
      if (!Util.isNullOrEmpty(ruleName)) {
        ruleObject = context.getObject(Rule.class, ruleName);
      }

      if (ruleObject == null) {
        log.error("no rule defined for approver " + approverName);
        throw new GeneralException("no rule defined for approver " + approverName);
      }

      Map<String, Object> ruleArgumentMap = new HashMap<String, Object>();
      ruleArgumentMap.put("log", log);
      ruleArgumentMap.put("identityName", approvalVariables.get("identityName"));
      ruleArgumentMap.put("identityDisplayName", approvalVariables.get("identityDisplayName"));
      ruleArgumentMap.put("launcher", approvalVariables.get("launcher"));
      ruleArgumentMap.put("approvalItem", approvalItem);
      ruleArgumentMap.put("requestObjectAttributes", approvalVariables.get("attributes"));
      ruleArgumentMap.put("owner", approvalVariables.get("owner"));
      ruleArgumentMap.put("fallbackApprover", approvalVariables.get("fallbackApprover"));
      ruleArgumentMap.put("context", context);

      result = (String) context.runRule(ruleObject, ruleArgumentMap);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new GeneralException(e);
    }

    if (log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "runRule", result));
    }
    return result;
  }
}
