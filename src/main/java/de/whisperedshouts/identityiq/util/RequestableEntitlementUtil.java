/**
 * 
 */
package de.whisperedshouts.identityiq.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import sailpoint.api.SailPointContext;
import sailpoint.object.Application;
import sailpoint.object.Custom;
import sailpoint.object.Identity;
import sailpoint.object.ManagedAttribute;
import sailpoint.object.ResourceObject;
import sailpoint.object.Rule;
import sailpoint.tools.GeneralException;
import sailpoint.tools.Util;

/**
 * @author mario.ragucci
 *
 */
public class RequestableEntitlementUtil {

  public final static String entitlementConfigurationName = "Requestable Entitlement Configuration";
  
  private final static Logger log  = Logger.getLogger(RequestableEntitlementUtil.class);
  private SailPointContext context = null;

  /**
   * @param context a SailPointContext
   */
  public RequestableEntitlementUtil(SailPointContext context) {
    this.context = context;
  }
  
  /**
   * @param requestableEntitlementConfig the Custom object containing the Entitlement Configuration
   * @param resourceObject the resource object, passed from the refresh task
   * @param accountGroup the accountGroup, passed from the refresh task
   * @param groupApplication the groupApplication,  passed from the refresh task
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  public void processEntitlementConfig(
      Custom requestableEntitlementConfig, 
      ResourceObject resourceObject, 
      ManagedAttribute accountGroup, 
      Application groupApplication) throws GeneralException 
  {    
    //getting information
    Map<String, Object> generalConfig = getGeneralConfig(requestableEntitlementConfig);
    Map<String, Map<String, Object>> appConfig = null;
    if(hasApplicationDefinition(requestableEntitlementConfig, groupApplication.getName())) {
      appConfig = getAppConfig(requestableEntitlementConfig, groupApplication.getName());
    }
    
    // general Configuration
    String governanceAttribute  = (String)  generalConfig.get("governanceAttribute");
    String defaultOwner         = (String)  generalConfig.get("defaultOwner");
    
    if(appConfig != null) {
      boolean configHit = false;
      Map<String, Object> appDefaultConfig = getAppDefaultConfig(appConfig, groupApplication.getName());
      
      if(appDefaultConfig == null) {
        String message = String.format("Config for application %s does not contain a default configuration",
            groupApplication.getName());
        log.error(message);
        throw new GeneralException(message);
      }
      
      if(appDefaultConfig.containsKey("defaultOwner")) {
        defaultOwner = String.valueOf(appDefaultConfig.get("defaultOwner"));
      }
      
      List<Map<String, Object>> entitlementConfigurationList = getAppEntitlementConfig(appConfig,
          groupApplication.getName());
      if (entitlementConfigurationList != null && !entitlementConfigurationList.isEmpty()) {
        for (Map<String, Object> entitlementConfiguration : entitlementConfigurationList) {
          configHit = isEntitlementMatchingConfig(
                          resourceObject, 
                          accountGroup, 
                          groupApplication,
                          entitlementConfiguration);
          
          if (configHit) {
            applyChangesToEntitlement(
                resourceObject, 
                accountGroup, 
                groupApplication, 
                entitlementConfiguration, 
                defaultOwner,
                governanceAttribute);
            break;
          }
        }
      }
      
      if(!configHit) {       
        applyChangesToEntitlement(accountGroup, appDefaultConfig, defaultOwner, governanceAttribute);
      }
      
      Boolean runAfterRule = false;
      if(getAppHasAfterRuleConfigured(appDefaultConfig)) {
        runAfterRule = Boolean.valueOf(String.valueOf(appDefaultConfig.get("runAfterRule")));
      }
      if(runAfterRule) {
        String afterRuleName  = String.valueOf(appDefaultConfig.get("runAfterRule"));
        accountGroup          = (ManagedAttribute) runRule(accountGroup, resourceObject, groupApplication, afterRuleName);
      }
      
    } else {
      applyChangesToEntitlement(accountGroup, generalConfig, "spadmin", governanceAttribute);
    }
  }

  /**
   * @param accountGroup the accountGroup object
   * @param appDefaultConfig the defaultConfiguration for the given application
   * @param fallbackOwnerName the fallback approver
   * @param governanceAttribute the attribute to set on the accountGroup
   * @throws GeneralException
   */
  private void applyChangesToEntitlement(
      ManagedAttribute accountGroup, 
      Map<String, Object> appDefaultConfig, 
      String fallbackOwnerName, 
      String governanceAttribute) throws GeneralException 
  {
    String  fallbackApprover      = (String)  appDefaultConfig.get("defaultOwner");
    String  defaultApprovalLevel  = (String)  appDefaultConfig.get("defaultApprovalLevel");
    Boolean isRequestable         = Boolean.valueOf((String) appDefaultConfig.get("isRequestable"));
    
    String ownerName = fallbackApprover;
    
    if(Util.isNullOrEmpty(fallbackApprover))  {
      log.error(String.format("No defaultApprover defined. Using fallback %s", fallbackOwnerName));
      ownerName = fallbackOwnerName;
    }
    
    Identity owner = context.getObject(Identity.class, ownerName);
    if(owner == null) {
      String message = String.format("Could not retrieve identity %s. fallbackApprover was %s", 
          ownerName, fallbackOwnerName);
      log.error(message);
      throw new GeneralException(message);
    }
    
    accountGroup.setOwner(owner);
    accountGroup.setAttribute(governanceAttribute, defaultApprovalLevel);
    accountGroup.setRequestable(isRequestable);
  }
  
  /**
   * @param resourceObject the resourceObject, passed from the refresh task
   * @param accountGroup the accountGroup object, passed from the refresh task
   * @param groupApplication the groupApplication object, passed from the refresh task
   * @param configuration the entitlement configuration being processed
   * @param fallbackOwnerName the fallback owner to use
   * @param governanceAttribute the governance attribute to set on the accountGroup
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  private void applyChangesToEntitlement(
      ResourceObject resourceObject, 
      ManagedAttribute accountGroup,
      Application groupApplication, 
      Map<String, Object> configuration, 
      String fallbackOwnerName, 
      String governanceAttribute) throws GeneralException 
  {    
    String   appGovValue    = (String) configuration.get("governanceLevel");
    String   selectionType  = (String) configuration.get("ownerSelectionType");
    String   staticOwner    = (String) configuration.get("staticOwner");
    String   ruleName       = (String) configuration.get("ownerSelectionRuleName");
    Boolean isRequestable   = Boolean.valueOf((String) configuration.get("isRequestable"));
    
    String ownerName = null;
    
    switch(selectionType.toLowerCase()) {
      case "static" : ownerName = staticOwner; break;
      case "rule"   : ownerName = (String) runRule(accountGroup, resourceObject, groupApplication, ruleName); break;
      default       : {
        String message = "Unknown selection type " + selectionType;
        log.error(message);
        throw new GeneralException(message);
      }
    }
    
    if(Util.isNullOrEmpty(ownerName))  {
      if(selectionType.equalsIgnoreCase("rule")) {
        String message = String.format("Rule ->%s<- did not return an owner. That's not a good thing. Using fallback approver %s",
            ruleName, fallbackOwnerName);
        log.error(message);
      }
      ownerName = fallbackOwnerName;
    }
    
    Identity owner = context.getObject(Identity.class, ownerName);
    if(owner == null) {
      String message = String.format("Could not retrieve identity %s. Selection type was %s, fallbackApprover was %s",
          ownerName, selectionType, fallbackOwnerName);
      log.error(message);
      throw new GeneralException(message);
    }
    
    accountGroup.setOwner(owner);
    accountGroup.setAttribute(governanceAttribute, appGovValue);
    accountGroup.setRequestable(isRequestable);
  }

  /**
   * @param config the Custom object containing the Entitlement Configuration
   * @param applicationName the application name being proessed
   * @return the Map containing the application configuration
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private Map<String, Map<String, Object>> getAppConfig(
      Custom config, 
      String applicationName) throws GeneralException 
  {
    Map<String, Map<String, Object>> result = null;
    Map<String, Map> appConfig = (Map<String, Map>) config.get("ApplicationConfiguration");
    if(appConfig == null) {
      String message = String.format("Custom configuration ->%s<- does not contain node %s", 
          config.getName(), "ApplicationConfiguration");
      log.error(message);
      throw new GeneralException(message);
    }
    
    if(isApplicationDefined(applicationName, appConfig)) {
      result = appConfig.get(applicationName);
    }
       
    return result;
  }

  /**
   * @param appConfig the Map containing the application configuration
   * @param applicationName the application name being processed
   * @return the Map with the default configuration for the given application
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  private Map<String, Object> getAppDefaultConfig(
      Map<String, Map<String, Object>> appConfig, 
      String applicationName) throws GeneralException 
  {
    Map<String, Object> result = new HashMap<>();
    if(!hasGeneralConfiguration(appConfig)) {
      String message = String.format("Configuration for appliation %s did not contain node %s", 
          applicationName, "GeneralConfiguration");
      log.error(message);
      throw new GeneralException(message);
    }
    result = (Map<String, Object>) appConfig.get("GeneralConfiguration");
    
    
    return result;
  }
  
  /**
   * @param appConfig the Map containing the application configuration
   * @param applicationName the appliation name
   * @return the List of configured entitlement configurations
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> getAppEntitlementConfig(
      Map<String, Map<String, Object>> appConfig, 
      String applicationName) throws GeneralException 
  {
    List<Map<String, Object>> result = new ArrayList<>();
    
    if(!hasEntitlementConfiguration(appConfig)) {
      String message = String.format("Configuration for appliation %s did not contain node %s", 
          applicationName, "EntitlementConfiguration");
      log.error(message);
      throw new GeneralException(message);
    }
    result = (List<Map<String, Object>>) appConfig.get("EntitlementConfiguration");
    
    
    return result;
  }

  /**
   * @param appDefaultConfig the defaultConfiguration for the given application
   * @return whether there is an after rule configured
   */
  private boolean getAppHasAfterRuleConfigured(Map<String, Object> appDefaultConfig) {
    return appDefaultConfig.containsKey("runAfterRule");
  }
  
  /**
   * @param config the Map containing the application configuration
   * @return the Map containing the fallback configuration
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> getGeneralConfig(Custom config) throws GeneralException {
    Map<String, Object> result        = null;
    Map<String, Object> generalConfig = (Map<String, Object>) config.get("GeneralConfiguration");
    if(generalConfig == null) {
      String message = String.format("Custom configuration ->%s<- does not contain node %s", 
          config.getName(), "GeneralConfiguration");      
      log.error(message);
      throw new GeneralException(message);
    }
    
    result = generalConfig;
       
    return result;
  }

  /**
   * @param accountGroup the accountGroup object, passed from the refresh task
   * @param regexAttribute the attribute of the accountGroup to check
   * @param regexValue the regex to execute
   * @return evaluation of the regular expression
   * @throws GeneralException when there was an issue
   */
  private boolean handleRegexSelection(
      ManagedAttribute accountGroup, 
      String regexAttribute, 
      String regexValue) throws GeneralException 
  {
    Boolean regexHit    = false;
    String valueToCheck = null;
    
    switch(regexAttribute.toLowerCase()) {
      case "displayname" : valueToCheck = accountGroup.getDisplayName(); break;
      case "value"       : valueToCheck = accountGroup.getValue(); break;
      case "type"        : valueToCheck = accountGroup.getType(); break;
      case "attribute"   : valueToCheck = accountGroup.getAttribute(); break;
      default            : valueToCheck = (String) accountGroup.get(regexAttribute); break;
    }
    
    if(Util.isNullOrEmpty(valueToCheck)) {
      String message = "managed attribute does not contain regexAttribute " + regexAttribute;
      log.error(message);
      throw new GeneralException(message);
    }
    
    regexHit = valueToCheck.matches(regexValue);
    
    return regexHit;
  }
  
  /**
   * @param appConfig the Map containing the application configuration
   * @param applicationName the application name being processed
   * @return true if the Entitlement Configuration contains a configuration entry for the given application
   */
  @SuppressWarnings("unchecked")
  private boolean hasApplicationDefinition(Custom config, String applicationName) {
    boolean hasDefinition = false;
    Map<String, Object> applicationConfiguration = null;
    if(config.containsAttribute("ApplicationConfiguration")) {
      applicationConfiguration = (Map<String, Object>) config.get("ApplicationConfiguration");
    }
    if(applicationConfiguration != null) {
      hasDefinition = applicationConfiguration.containsKey(applicationName);
    }   
    
    return hasDefinition;
  }
  
  /**
   * @param appConfig
   * @return
   */
  private boolean hasEntitlementConfiguration(Map<String, Map<String, Object>> appConfig) {
    return appConfig.containsKey("EntitlementConfiguration");
  }
  
  /**
   * @param appConfig
   * @return
   */
  private boolean hasGeneralConfiguration(Map<String, Map<String, Object>> appConfig) {
    return appConfig.containsKey("GeneralConfiguration");
  }

  /**
   * @param applicationName
   * @param appConfig
   * @return
   */
  @SuppressWarnings("rawtypes")
  private boolean isApplicationDefined(String applicationName, Map<String, Map> appConfig) {
    return appConfig.containsKey(applicationName);
  }
  
  /**
   * @param resourceObject the resourceObject, passed from the refresh task
   * @param accountGroup the accountGroup object, passed from the refresh task
   * @param groupApplication the groupApplication object, passed from the refresh task
   * @param configuration the entitlement configuration being processed
   * @return true if the processed object is selected by the entitlement selector
   * @throws GeneralException when there was an issue getting a SailPointObject
   */
  private boolean isEntitlementMatchingConfig(
      ResourceObject resourceObject,
      ManagedAttribute accountGroup,
      Application groupApplication, 
      Map<String, Object> configuration) throws GeneralException 
  {
    boolean configHit         = false;
    String selectionType      = (String)  configuration.get("selectionType");
    String regexAttribute     = (String)  configuration.get("selectionRegexAttribute");
    String regexValue         = (String)  configuration.get("selectionRegexValue");
    String ruleName           = (String)  configuration.get("selectionRuleName");
    
    if(Util.isNullOrEmpty(selectionType)) {
      String message = "empty selectionType";
      log.error(message);
      throw new GeneralException(message);
    }
    switch(selectionType.toLowerCase()) {
      case "regex" : configHit = handleRegexSelection(accountGroup, regexAttribute, regexValue); break;
      case "rule"  : configHit = (boolean) runRule(accountGroup, resourceObject, groupApplication, ruleName); break;
      default      : {
        String message = "Unknown selection type " + selectionType;
        log.error(message);
        throw new GeneralException(message);
      }
    }
    return configHit;
  }
  
  /**
   * @param accountGroup the accountGroup object, passed from the refresh task
   * @param obj the resourceObject, passed from the refresh task
   * @param groupApplication the groupApplication object, passed from the refresh task
   * @param ruleName the rule to call
   * @return the return value of the rule
   * @throws GeneralException when there was an issue with the rule
   */
  private Object runRule(
      ManagedAttribute accountGroup, 
      ResourceObject obj, 
      Application groupApplication,
      String ruleName) throws GeneralException 
  {
    Object result = null;
    Rule rule     = context.getObject(Rule.class, ruleName);
    
    if(rule != null) {
      Map<String, Object> arguments = new HashMap<>();
      arguments.put("context", context);
      arguments.put("log", log);
      arguments.put("accountGroup", accountGroup);
      arguments.put("obj", obj);
      arguments.put("groupApplication", groupApplication);
      
      result = context.runRule(rule, arguments);
    }
    
    return result;
  }
}
