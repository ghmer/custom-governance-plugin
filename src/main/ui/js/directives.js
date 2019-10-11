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
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/approvals/approvalLevelOverviewDirective.html')
    };
  });

  app.directive('customGovernanceApprovalRules', function() {
    return {
      controller: 'HomeController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/approvals/approverLookupRulesOverviewDirective.html')
    };
  });

  app.directive('applyChangesButton', function() {
    return {
      controller: 'HomeController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/approvals/applyChangesButtonDirective.html')
    };
  });
  
  app.directive('customGovernanceSetup', function() {
    return {
      controller: 'SetupController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/setup/setupDirective.html')
    };
  });
  
  app.directive('setupWorkflowIntegration', function() {
    return {
      controller: 'SetupController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/setup/setupWorkflowIntegrationDirective.html')
    };
  });
  
  app.directive('setupWorkflowApprovalMode', function() {
    return {
      controller: 'SetupController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/setup/setupWorkflowApprovalModeDirective.html')
    };
  });
  
  app.directive('setupAggregationTasks', function() {
    return {
      controller: 'SetupController',
      controllerAs: 'controller',
      restrict: 'E',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/setup/setupAggregationTasksDirective.html')
    };
  });
  
  app.directive('entitlementGeneralConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/entitlements/generalConfigurationDirective.html')
    };
  });

  app.directive('applicationGeneralConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/entitlements/applicationGeneralConfigurationDirective.html')
    };
  });

  app.directive('applicationEntitlementConfiguration', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/entitlements/entitlementConfigurationDirective.html')
    };
  });

  app.directive('definedEntitlementApplications', function() {
    return {
      controller: 'EntitlementController',
      controllerAs: 'controller',
      templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/directives/entitlements/definedApplicationsDirective.html')
    };
  });
  
}());