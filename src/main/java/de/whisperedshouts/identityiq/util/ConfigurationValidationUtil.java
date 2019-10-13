/**
 * 
 */
package de.whisperedshouts.identityiq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.whisperedshouts.identityiq.rest.CGPRestInterface;
import sailpoint.api.SailPointContext;
import sailpoint.object.Filter;
import sailpoint.object.Identity;
import sailpoint.object.QueryOptions;
import sailpoint.object.Rule;
import sailpoint.tools.GeneralException;

/**
 * @author mario.ragucci
 *
 */
public class ConfigurationValidationUtil {
  
  private static final Logger log = Logger.getLogger(ConfigurationValidationUtil.class);
  
  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @param configType the Config Type to be checked. May be approvallevel or entitlement
   * @return a Map containing information about the validated object
   * @throws GeneralException in case of an issue
   */
  public static Map<String, Object> validate(
      SailPointContext context, 
      Map<String, Object> configuration, 
      String configType) throws GeneralException 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s, configType = %s)", 
          "validate", 
          context,
          configuration,
          configType));
    }
    Map<String, Object> result = new HashMap<>();
    switch(configType.toLowerCase()) {
      case "approvallevel" : result = validateGovernanceConfiguration(context, configuration);  break;
      case "entitlement"   : result = validateEntitlementConfiguration(context, configuration); break;
      default : throw new GeneralException("Configuration type " + configType + " not supported");
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validate", result));
    }
    return result;
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @return a Map containing information about the validated object
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> validateEntitlementConfiguration(
      SailPointContext context, 
      Map<String, Object> configuration) 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s)", 
          "validateEntitlementConfiguration", 
          context,
          configuration));
    }
    Map<String, Object> infoMap   = new HashMap<>();
    List<String> approvalLevels   = new ArrayList<>();
    try {
      approvalLevels   = CGPRestInterface.getApprovalLevels(context);
    } catch (GeneralException e) {
      log.error(e.getMessage());
    }
    validateEntitlementConfigurationGeneralSection(context, configuration, approvalLevels, infoMap);
    validateEntitlementConfigurationApplicationSection(context, configuration, approvalLevels, infoMap);
    
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    infoMap.put("isValid", errorMessages.isEmpty());
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateEntitlementConfiguration", infoMap));
    }
    return infoMap;
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @param approvalLevels a list of valid approval levels
   * @param infoMap a Map containing information about the validated object
   */
  @SuppressWarnings("unchecked")
  private static void validateEntitlementConfigurationApplicationSection(
      SailPointContext context, 
      Map<String, Object> configuration,
      List<String> approvalLevels, 
      Map<String, Object> infoMap) 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s, approvalLevels %s, infoMap = %s)", 
          "validateEntitlementGeneralSection", 
          context,
          configuration,
          approvalLevels,
          infoMap));
    }
    
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    if(errorMessages == null) {
      errorMessages = new ArrayList<>();
    }
    
    if(!(configuration.containsKey("ApplicationConfiguration"))) {
      String message = "RootNode: Configuration does not contain the key ApplicationConfiguration";
      log.error(message);
      errorMessages.add(message);
    } else {
      Map<String, Map<String, Object>> applicationConfiguration = (Map<String, Map<String, Object>>) configuration.get("ApplicationConfiguration");
      
      if(applicationConfiguration != null && !(applicationConfiguration.isEmpty())) {
        for(Map.Entry<String, Map<String, Object>> application : applicationConfiguration.entrySet()) {
          String applicationName = application.getKey();
          Map<String, Object> definition = application.getValue();
          // validate the application general configuration
          if(definition.containsKey("GeneralConfiguration")) {
            validateApplicationGeneralConfig(context, approvalLevels, errorMessages, applicationName, definition);
          }
          // validate the entitlement configuration section
          if(definition.containsKey("EntitlementConfiguration")) {
            validateApplicationEntitlementConfig(context, approvalLevels, errorMessages, applicationName, definition);     
          }
        }
      }  
    }
    
    infoMap.put("isValid", errorMessages.isEmpty());
    infoMap.put("errorMessages", errorMessages);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateEntitlementGeneralSection", null));
    }
    
  }

  /**
   * @param context a SailPointContext
   * @param approvalLevels a list of valid approval levels
   * @param errorMessages a list of encountered errors
   * @param applicationName the application name being checked
   * @param definition the definition of this applications entitlement configuration
   * @throws IllegalArgumentException if there was an issue
   */
  @SuppressWarnings("unchecked")
  private static void validateApplicationGeneralConfig(
      SailPointContext context, 
      List<String> approvalLevels,
      List<String> errorMessages, 
      String applicationName, 
      Map<String, Object> definition)
      throws IllegalArgumentException 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(context = %s, "
          + "approvalLevels %s, "
          + "errorMessages %s, "
          + "applicationName %s, "
          + "definition = %s)", 
          "validateApplicationGeneralConfig", 
          context,
          approvalLevels,
          errorMessages,
          applicationName,
          definition));
    }
    Map<String, Object> generalConfiguration = (Map<String, Object>) definition.get("GeneralConfiguration");
    
    String defaultApprovalLevel = String.valueOf(generalConfiguration.get("defaultApprovalLevel"));
    if(defaultApprovalLevel == null || defaultApprovalLevel.isEmpty()) {
      String message = String.format(
          "GeneralConfiguration: Application %s has empty default approval Level",
          applicationName);
      log.error(message);
      errorMessages.add(message);
    } else {
      if(!(approvalLevels.contains(defaultApprovalLevel))) {
        String message = String.format(
            "GeneralConfiguration: Application %s has an invalid default approval level %s",
            applicationName,
            defaultApprovalLevel);
        log.error(message);
        errorMessages.add(message);
      }
    }
    
    String defaultOwner = String.valueOf(generalConfiguration.get("defaultOwner"));
    if(defaultOwner == null || defaultOwner.isEmpty()) {
      String message = String.format(
          "GeneralConfiguration: Application %s has empty default owner",
          applicationName);
      log.error(message);
      errorMessages.add(message);
    } else {
      int count = checkIdentityCount(context, errorMessages, defaultOwner);
      if(count != 1) {
        String message = String.format(
            "GeneralConfiguration: Application %s has an invalid default owner %s",
            applicationName,
            defaultOwner);
        log.error(message);
        errorMessages.add(message);
      } 
    }
    
    if(!(generalConfiguration.containsKey("isRequestable"))) {
      String message = String.format(
          "GeneralConfiguration: Application %s has no selection for is requestable",
          applicationName);
      log.error(message);
      errorMessages.add(message); 
    }
    
    if(!(generalConfiguration.containsKey("runAfterRule"))) {
      String message = String.format(
          "GeneralConfiguration: Application %s has no selection for run after aggregation rule",
          applicationName);
      log.error(message);
      errorMessages.add(message);
    } else {
      Boolean runAfterRule = Boolean.valueOf(String.valueOf(generalConfiguration.get("runAfterRule")));
      if(runAfterRule) {
        String afterRuleName = String.valueOf(generalConfiguration.get("afterRuleName"));
        if(afterRuleName == null || afterRuleName.isEmpty()) {
          String message = String.format(
              "GeneralConfiguration: Application %s has an empty run after rule",
              applicationName);
          log.error(message);
          errorMessages.add(message);
        } else {
          int count = checkRuleCount(context, errorMessages, afterRuleName);
          
          if(count != 1) {
            String message = String.format(
                "GeneralConfiguration: Application %s has an invalid run after aggregation rule %s",
                applicationName,
                afterRuleName);
            log.error(message);
            errorMessages.add(message);
          }
        }
      }
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateApplicationGeneralConfig", null));
    }
  }

  /**
   * @param context a SailPointContext
   * @param approvalLevels a list of valid approval levels
   * @param errorMessages a list of encountered errors
   * @param applicationName the name of the application being checked
   * @param definition the application's entitlement configuration
   * @throws IllegalArgumentException if there was an issue
   */
  @SuppressWarnings("unchecked")
  private static void validateApplicationEntitlementConfig(
      SailPointContext context, 
      List<String> approvalLevels,
      List<String> errorMessages, 
      String applicationName, 
      Map<String, Object> definition)
      throws IllegalArgumentException 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s("
          + "context = %s, "
          + "approvalLevels = %s, "
          + "errorMessages = %s, "
          + "applicationName = %s, "
          + "definition = %s)", 
          "validateApplicationEntitlementConfig", 
          context,
          approvalLevels,
          errorMessages,
          applicationName,
          definition));
    }
    List<Map<String, Object>> entitlementConfiguration = (List<Map<String, Object>>) definition.get("EntitlementConfiguration");
    
    if(entitlementConfiguration != null && !entitlementConfiguration.isEmpty()) {
      for(Map<String, Object> entitlementDefinition : entitlementConfiguration) {
        String descriptor = String.valueOf(entitlementDefinition.get("descriptor"));
        if(descriptor == null || descriptor.isEmpty()) {
          String message = String.format(
              "EntitlementConfiguration: Application %s has a definition with an empty name/descriptor",
              applicationName);
          log.error(message);
          errorMessages.add(message);
        }
        
        if(!(entitlementDefinition.containsKey("isRequestable"))) {
          String message = String.format(
              "EntitlementConfiguration: Application %s has definition %s with no selection for is requestable",
              applicationName,
              descriptor);
          log.error(message);
          errorMessages.add(message); 
        }
        
        String governanceLevel = String.valueOf(entitlementDefinition.get("governanceLevel"));
        if(governanceLevel == null || governanceLevel.isEmpty()) {
          String message = String.format(
              "EntitlementConfiguration: Application %s has definition %s with an empty approval level",
              applicationName,
              descriptor);
          log.error(message);
          errorMessages.add(message);
        } else {
          if(!(approvalLevels.contains(governanceLevel))) {
            String message = String.format(
                "EntitlementConfiguration: Application %s has definition %s with an invalid approval level %s",
                applicationName,
                descriptor,
                governanceLevel);
            log.error(message);
            errorMessages.add(message);
          }
        }
       
        String ownerSelectionType = String.valueOf(entitlementDefinition.get("ownerSelectionType"));
        if(ownerSelectionType == null || ownerSelectionType.isEmpty()) {
          String message = String.format(
              "EntitlementConfiguration: Application %s has definition %s with an empty owner selection type",
              applicationName,
              descriptor);
          log.error(message);
          errorMessages.add(message);
        } else {
          List<String> ownerSelectionTypes = new ArrayList<>();
          ownerSelectionTypes.add("rule");
          ownerSelectionTypes.add("static");
          if(!(ownerSelectionTypes.contains(ownerSelectionType))) {
            String message = String.format(
                "EntitlementConfiguration: Application %s has definition %s with an invalid owner selection type %s",
                applicationName,
                descriptor,
                ownerSelectionType);
            log.error(message);
            errorMessages.add(message);
          }
          
          switch(ownerSelectionType) {
            case "rule"   : {
              String ownerSelectionRuleName = String.valueOf(entitlementDefinition.get("ownerSelectionRuleName"));
              if(ownerSelectionRuleName == null || ownerSelectionRuleName.isEmpty()) {
                String message = String.format(
                    "EntitlementConfiguration: Application %s has definition %s with an empty owner selection rule",
                    applicationName,
                    descriptor);
                log.error(message);
                errorMessages.add(message);
              } else {
                int count = checkRuleCount(context, errorMessages, ownerSelectionRuleName);
                if(count != 1) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an invalid selection rule %s",
                      applicationName,
                      descriptor,
                      ownerSelectionRuleName);
                  log.error(message);
                  errorMessages.add(message);
                }
              }
              break;
            }
            case "static" : {
              String staticOwner = String.valueOf(entitlementDefinition.get("staticOwner"));
              if(staticOwner == null || staticOwner.isEmpty()) {
                String message = String.format(
                    "EntitlementConfiguration: Application %s has definition %s with an empty static owner",
                    applicationName,
                    descriptor);
                log.error(message);
                errorMessages.add(message);
              } else {
                int count = checkIdentityCount(context, errorMessages, staticOwner);
                
                if(count != 1) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an invalid static owner %s",
                      applicationName,
                      descriptor,
                      staticOwner);
                  log.error(message);
                  errorMessages.add(message);
                }
              }
              break;
            }
          }
        }
        
        String selectionType = String.valueOf(entitlementDefinition.get("selectionType"));
        if(selectionType == null || selectionType.isEmpty()) {
          String message = String.format(
              "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection type",
              applicationName,
              descriptor);
          log.error(message);
          errorMessages.add(message);
        } else {
          List<String> selectionTypes = new ArrayList<>();
          selectionTypes.add("rule");
          selectionTypes.add("regex");
          if(!(selectionTypes.contains(selectionType))) {
            String message = String.format(
                "EntitlementConfiguration: Application %s has definition %s with an invalid eligible selection type %s",
                applicationName,
                descriptor,
                selectionType);
            log.error(message);
            errorMessages.add(message);
          } else {
            switch(selectionType) {
              case "rule"   : {
                if(!(entitlementDefinition.containsKey("selectionRuleName"))) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection rule",
                      applicationName,
                      descriptor);
                  log.error(message);
                  errorMessages.add(message);
                }
                String selectionRuleName = String.valueOf(entitlementDefinition.get("selectionRuleName"));
                if(selectionRuleName == null || selectionRuleName.isEmpty()) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection rule",
                      applicationName,
                      descriptor);
                  log.error(message);
                  errorMessages.add(message);
                } else {
                  int count = checkRuleCount(context, errorMessages, selectionRuleName);
                  
                  if(count != 1) {
                    String message = String.format(
                        "EntitlementConfiguration: Application %s has definition %s with an invalid eligible selection rule %s",
                        applicationName,
                        descriptor,
                        selectionRuleName);
                    log.error(message);
                    errorMessages.add(message);
                  }
                }
                break;
              }
              case "regex" : {
                if(!(entitlementDefinition.containsKey("selectionRegexAttribute"))) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection regex attribute",
                      applicationName,
                      descriptor);
                  log.error(message);
                  errorMessages.add(message);
                }
                String selectionRegexAttribute = String.valueOf(entitlementDefinition.get("selectionRegexAttribute"));
                if(selectionRegexAttribute == null || selectionRegexAttribute.isEmpty()) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection regex attribute %s",
                      applicationName,
                      descriptor,
                      selectionRegexAttribute);
                  log.error(message);
                  errorMessages.add(message);
                }
                
                if(!(entitlementDefinition.containsKey("selectionRegexValue"))) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection regex value",
                      applicationName,
                      descriptor);
                  log.error(message);
                  errorMessages.add(message);
                }
                String selectionRegexValue = String.valueOf(entitlementDefinition.get("selectionRegexValue"));
                if(selectionRegexValue == null || selectionRegexValue.isEmpty()) {
                  String message = String.format(
                      "EntitlementConfiguration: Application %s has definition %s with an empty eligible selection regex value %s",
                      applicationName,
                      descriptor,
                      selectionRegexValue);
                  log.error(message);
                  errorMessages.add(message);
                } else {
                  try {
                    @SuppressWarnings("unused")
                    Pattern pattern = Pattern.compile(selectionRegexValue);
                  } catch(Exception e) {
                    String message = String.format(
                        "EntitlementConfiguration: Application %s has definition %s with an invalid regex %s. Error: %s",
                        applicationName,
                        descriptor,
                        selectionRegexValue,
                        e.getMessage());
                    log.error(message);
                    errorMessages.add(message);
                  }
                }
                break;
              }
              default: log.error("Invalid selection type " + selectionType);
            }
          }
        }
      }
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateApplicationEntitlementConfig", null));
    }
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @param approvalLevels a list of valid approval levels
   * @param infoMap a map containing information about the object being checked
   */
  @SuppressWarnings("unchecked")
  private static void validateEntitlementConfigurationGeneralSection(
      SailPointContext context, 
      Map<String, Object> configuration,
      List<String> approvalLevels, 
      Map<String, Object> infoMap) 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s("
          + "context = %s, "
          + "configuration = %s, "
          + "approvalLevels %s, "
          + "infoMap = %s)", 
          "validateEntitlementGeneralSection", 
          context,
          configuration,
          approvalLevels,
          infoMap));
    }
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    if(errorMessages == null) {
      errorMessages = new ArrayList<>();
    }
    
    if(!(configuration.containsKey("GeneralConfiguration"))) {
      String message = "RootNode: Configuration does not contain the key GeneralConfiguration";
      log.error(message);
      errorMessages.add(message);
    } else {
      Map<String, Object> generalConfiguration = (Map<String, Object>) configuration.get("GeneralConfiguration");
      
      if(!(generalConfiguration.containsKey("defaultApprovalLevel"))) {
        String message = "GeneralConfiguration: Settings do not contain the key defaultApprovalLevel";
        log.error(message);
        errorMessages.add(message);
      } else {
        String approvalLevel = String.valueOf(generalConfiguration.get("defaultApprovalLevel"));
        if(!(approvalLevels.contains(approvalLevel))) {
          String message = "GeneralConfiguration: Invalid defaultApprovalLevel " + approvalLevel;
          log.error(message);
          errorMessages.add(message);
        }
      }
      
      if(!(generalConfiguration.containsKey("governanceAttribute"))) {
        String message = "GeneralConfiguration: Settings do not contain the key governanceAttribute";
        log.error(message);
        errorMessages.add(message);
      } else {
        String governanceAttribute = String.valueOf(generalConfiguration.get("governanceAttribute"));
        if(governanceAttribute == null || governanceAttribute.isEmpty()) {
          String message = "GeneralConfiguration: Invalid governanceAttribute " + governanceAttribute;
          log.error(message);
          errorMessages.add(message);
        }
      }
      
      if(!(generalConfiguration.containsKey("defaultOwner"))) {
        String message = "GeneralConfiguration: Settings do not contain the key defaultOwner";
        log.error(message);
        errorMessages.add(message);
      } else {
        String defaultOwner = String.valueOf(generalConfiguration.get("defaultOwner"));
        int count = checkIdentityCount(context, errorMessages, defaultOwner);
        if(count != 1) {
          String message = "GeneralConfiguration: Invalid defaultOwner " + defaultOwner;
          log.error(message);
          errorMessages.add(message);
        }
      }
      
      if(!(generalConfiguration.containsKey("isRequestable"))) {
        String message = "GeneralConfiguration: no selection for is requestable";
        log.error(message);
        errorMessages.add(message); 
      }
      
    }
    
    infoMap.put("isValid", errorMessages.isEmpty());
    infoMap.put("errorMessages", errorMessages);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateEntitlementGeneralSection", null));
    }
  }

  /**
   * @param context a SailPointContext
   * @param errorMessages a list of encountered errors
   * @param identityName the name of the identity being checked
   * @return the number of identities found in the system matching the identityName
   * @throws IllegalArgumentException if there was an issue
   */
  private static int checkIdentityCount(
      SailPointContext context, 
      List<String> errorMessages, 
      String identityName)
      throws IllegalArgumentException 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format(
          "ENTERING %s(context = %s, errorMessages = %s, identityName = %s)", 
          "checkIdentity", context, errorMessages, identityName));
    }
    QueryOptions queryOptions = new QueryOptions();
    queryOptions.addFilter(Filter.eq("name", identityName));
    
    int count = 0;
    try {
      count = context.countObjects(Identity.class, queryOptions);
    } catch (GeneralException e) {
      String message = String.format(
          "General Error: error while getting identity %s: %s", 
          identityName, 
          e.getMessage());
      log.error(message);
      errorMessages.add(message);
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "checkIdentity", count));
    }
    return count;
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @return a Map containing information about the object being checked
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> validateGovernanceConfiguration(
      SailPointContext context, 
      Map<String, Object> configuration) 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s)", 
          "validateGovernanceConfiguration", 
          context,
          configuration));
    }
    
    Map<String, Object> infoMap = new HashMap<>();
    validateGovernanceConfigurationApproverLookups(context, configuration, infoMap);
    validateGovernanceConfigurationApprovalLevels(context, configuration, infoMap);
    
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    infoMap.put("isValid", errorMessages.isEmpty());
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateGovernanceConfiguration", infoMap));
    }
    return infoMap;
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @param infoMap a Map containing information about the object being checked
   */
  @SuppressWarnings("unchecked")
  private static void validateGovernanceConfigurationApprovalLevels(
      SailPointContext context, 
      Map<String, Object> configuration,
      Map<String, Object> infoMap) {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s, infoMap = %s)", 
          "validateApprovalLevels", 
          context,
          configuration,
          infoMap));
    }
    
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    if(errorMessages == null) {
      errorMessages = new ArrayList<>();
    }
    if(!(configuration.containsKey("approvalLevels"))) {
      errorMessages.add("configuration does not contain the >approverLookupRules< key.");
    } else {
      
      List<String> approverList = (List<String>) infoMap.get("approverList");
      Map<String, Map<String, List<String>>> approvalLevels = 
          (Map<String, Map<String, List<String>>>) configuration.get("approvalLevels");
      
      for(String approvalLevel : approvalLevels.keySet()) {
        Map<String,List<String>> approvalLevelMap = approvalLevels.get(approvalLevel);
        
        if(!(approvalLevelMap.containsKey("approval"))) {
          String message = String.format("configuration does contain approvalLevel %s without an entry for approvals",
              approvalLevel);
          log.error(message);
          errorMessages.add(message);
          
        } else {
          List<String> approvers = approvalLevelMap.get("approval");
          
          for(String approver : approvers) {
            if(!(approverList.contains(approver))) {
              String message = String.format("approvalLevel %s contains illegal approver %s.",
                  approvalLevel, approver);
              log.error(message);
              errorMessages.add(message);
            }
          }
          
        }        
      }
    }
    
    infoMap.put("isValid", errorMessages.isEmpty());
    infoMap.put("errorMessages", errorMessages);
    
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateApprovalLevels", null));
    }
  }

  /**
   * @param context a SailPointContext
   * @param configuration a Map containing the configuration parameters
   * @param infoMap a Map containing information about the object being checked
   * @throws IllegalArgumentException if there was an issue
   */
  @SuppressWarnings("unchecked")
  private static void validateGovernanceConfigurationApproverLookups(
      SailPointContext context, 
      Map<String, Object> configuration, 
      Map<String, Object> infoMap)
      throws IllegalArgumentException 
  {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, configuration = %s, infoMap = %s)", 
          "validateApproverLookups", 
          context,
          configuration,
          infoMap));
    }
       
    List<String> errorMessages = (List<String>) infoMap.get("errorMessages");
    if(errorMessages == null) {
      errorMessages = new ArrayList<>();
    }
    List<String> approverList  = new ArrayList<>();
    //validate approver lookup rules
    if(!(configuration.containsKey("approverLookupRules"))) {
      errorMessages.add("configuration does not contain the >approverLookupRules< key.");
    } else {
      Map<String, String> approverLookupRules = (Map<String, String>) configuration.get("approverLookupRules");
      for(Map.Entry<String, String> approverLookup : approverLookupRules.entrySet()) {
        String approver = approverLookup.getKey();
        String ruleName = approverLookup.getValue();
        
        approverList.add(approver);
        int count = checkRuleCount(context, errorMessages, ruleName);
        
        if(count != 1) {
          String message = String.format("configuration contains rule %s for approver %s which seems to be invalid.",
              ruleName,
              approver);
          log.error(message);
          errorMessages.add(message);
        }
        checkRuleCount(context, errorMessages, ruleName);
      }
    }

    infoMap.put("isValid", errorMessages.isEmpty());
    infoMap.put("errorMessages", errorMessages);
    infoMap.put("approverList", approverList);
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "validateApproverLookups", null));
    }
  }

  /**
   * @param context a SailPointContext
   * @param errorMessages a list of encountered errors
   * @param ruleName the name of the rule being checked
   * @return the number of rules matching that name
   * @throws IllegalArgumentException
   */
  private static int checkRuleCount(SailPointContext context, List<String> errorMessages, String ruleName)
      throws IllegalArgumentException {
    if(log.isDebugEnabled()) {
      log.debug(String.format("ENTERING %s(context = %s, errorMessages = %s, ruleName = %s)", 
          "checkRule", 
          context,
          errorMessages,
          ruleName));
    }
    QueryOptions ruleNameOptions = new QueryOptions();
    ruleNameOptions.addFilter(Filter.eq("name", ruleName));
    int objectCount = 0;
    try {
      objectCount = context.countObjects(Rule.class, ruleNameOptions);
      if(objectCount != 1) {
        String message = String.format("Rule %s seems not to be available.",
            "approverLookupRules",
            ruleName);
        log.error(message);
        errorMessages.add(message);
      }
      
    } catch (GeneralException e) {
      String message = String.format("Error in %s: while getting rule %s: %s",
          "approverLookupRules",
          ruleName,
          e.getMessage());
      errorMessages.add(message);
      log.error(message);
      errorMessages.add(message);
    }
    
    if(log.isDebugEnabled()) {
      log.debug(String.format("LEAVING %s(return = %s)", "checkRule", objectCount));
    }
    return objectCount;
  }

}
