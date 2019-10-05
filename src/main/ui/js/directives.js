(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp');

  app.directive('customGovernanceNavigation', function() {
    return {
      controller: 'NavigateController',
      controllerAs: 'controller',
      restrict: 'E',
      scope: {
        activelink: '@'
      },
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/navigationDirective.html')
    };
  });

  app.directive('customGovernanceApprovalLevels', function() {
    return {
      controller: 'HomeController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/approvalLevelOverviewDirective.html')
    };
  });

  app.directive('customGovernanceApprovalRules', function() {
    return {
      controller: 'HomeController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/approverLookupRulesOverviewDirective.html')
    };
  });

  app.directive('applyChangesButton', function() {
    return {
      controller: 'HomeController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/applyChangesButtonDirective.html')
    };
  });
  
  app.directive('customGovernanceSetup', function() {
    return {
      controller: 'SetupController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/setupDirective.html')
    };
  });
  
  app.directive('entitlementGeneralConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/generalConfigurationDirective.html')
    };
  });

  app.directive('applicationGeneralConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/applicationGeneralConfigurationDirective.html')
    };
  });

  app.directive('applicationEntitlementConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/entitlementConfigurationDirective.html')
    };
  });

  app.directive('definedEntitlementApplications', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/definedApplicationsDirective.html')
    };
  });
  
}());