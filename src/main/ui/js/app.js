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

      .when('/admin', {
        templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/partials/admin.html'),
        controller: 'HomeController'
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