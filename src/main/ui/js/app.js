(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp', ['ngRoute', 'ui.bootstrap', 'checklist-model']);
  app.config(function($routeProvider) {
    $routeProvider

      .when('/', {
        templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/manage.html'),
        controller: 'HomeController'
      })

      .when('/manage', {
        templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/manage.html'),
        controller: 'HomeController'
      })

      .when('/setup', {
        templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/setup.html'),
        controller: 'SetupController'
      })
      
      .when('/entitlements', {
        templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/entitlements.html'),
        controller: 'EntitlementController'
      })

      .otherwise({
        redirectTo: '/'
      });
  });

  //this fixes the escaped slashes
  app.config(['$locationProvider', function($locationProvider) {
    $locationProvider.hashPrefix('');
  }]);
}());