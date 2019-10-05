(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp');

  app.service('GovernancePluginService', function($http, $q) {

    this.validateNotInList = function(list, value) {
      var notInList = true;
      for (var i = 0; i < list.length; i++) {
        if (value == list[i]) {
          notInList = false;
          break;
        }
      }
      return notInList;
    };

    this.isInList = function(list, value) {
      var isInList = false;
      for (var i = 0; i < list.length; i++) {
        if (value == list[i]) {
          isInList = true;
          break;
        }
      }
      return isInList;
    };
    
    this.removeEntries = function(lista, listb) {
      var newArray = [...lista];
      newArray = newArray.filter( function(el) {
        return !listb.includes( el );
      } );
      
      return newArray;
    };

    this.getArrayExceptValue = function(value, array) {
      var newArray = [...array];
      var found = false;
      var index = -1;
      for (var i = 0; i < newArray.length; i++) {
        if (value == newArray[i]) {
          index = i;
          found = true;
          break;
        }
      }
      if (found) {
        newArray.splice(index, 1);
      }

      return newArray;
    };
    
    this.raise = function(object, index) {
      var temp1 = object[index - 1];
      var temp2 = object[index];
      object[index - 1] = temp2;
      object[index]     = temp1;
    };

    this.lower = function(object, index) {
      var temp1 = object[index + 1];
      var temp2 = object[index];
      object[index + 1] = temp2;
      object[index]     = temp1;
    };
    
    this.getAvailableRules = function(type) {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url: PluginHelper.getPluginRestUrl('custom-governance') + '/ruleNames/' + type
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        deferred.reject(response);
        return deferred.promise;
      });
    };
    
    this.getAvailableApplications = function(templateName) {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url: PluginHelper.getPluginRestUrl('custom-governance') + '/applicationNames'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        deferred.reject(response);
        return deferred.promise;
      });
    };
    
    this.getGovernanceModel = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url : PluginHelper.getPluginRestUrl('custom-governance') + '/governanceModel'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        // the following line rejects the promise 
        deferred.reject(response);
        // promise is returned
        return deferred.promise;
      });
    };
    
    this.getApprovalLevels = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url : PluginHelper.getPluginRestUrl('custom-governance') + '/governanceModel/approvalLevels',
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        // the following line rejects the promise 
        deferred.reject(response);
        // promise is returned
        return deferred.promise;
      });
    };

    this.saveGovernanceModel = function(governanceModel) {
      var deferred  = $q.defer(); 
      return $http({
        method  : "POST",
            withCredentials: true,
            xsrfHeaderName : "X-XSRF-TOKEN",
            xsrfCookieName : "CSRF-TOKEN",
            url : PluginHelper.getPluginRestUrl('custom-governance') + '/governanceModel/update',
            headers: {'Content-Type': 'application/json'},
            data : governanceModel
      }).then(function mySuccess(response) {
          deferred.resolve(response.data);
          console.log(deferred.promise);
            return deferred.promise;
        }, function myError(response) {
            deferred.reject(response);
            return deferred.promise;
        });
    };
    
    this.getSetupInformation = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url : PluginHelper.getPluginRestUrl('custom-governance') + '/setup/information'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        // the following line rejects the promise 
        deferred.reject(response);
        // promise is returned
        return deferred.promise;
      });
    };
    
    this.revertIntegrationStatus = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url: PluginHelper.getPluginRestUrl('custom-governance') + '/setup/revertIntegrationStatus'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        deferred.reject(response);
        return deferred.promise;
      });
    };
    
    this.performIntegration = function(setupInformation) {
      var deferred  = $q.defer(); 
      return $http({
        method  : "POST",
            withCredentials: true,
            xsrfHeaderName : "X-XSRF-TOKEN",
            xsrfCookieName : "CSRF-TOKEN",
            url: PluginHelper.getPluginRestUrl('custom-governance') + '/setup/performIntegration',
            headers: {'Content-Type': 'application/json'},
            data : setupInformation
      }).then(function mySuccess(response) {
          deferred.resolve(response.data);
          console.log(deferred.promise);
            return deferred.promise;
        }, function myError(response) {
            deferred.reject(response);
            return deferred.promise;
        });
    };
    
    this.getEntitlementConfiguration = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url : PluginHelper.getPluginRestUrl('custom-governance') + '/entitlementConfiguration'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        // the following line rejects the promise 
        deferred.reject(response);
        // promise is returned
        return deferred.promise;
      });
    };
    
    this.saveEntitlementConfiguration = function(entitlementConfiguration) {
      var deferred  = $q.defer(); 
      return $http({
        method  : "POST",
            withCredentials: true,
            xsrfHeaderName : "X-XSRF-TOKEN",
            xsrfCookieName : "CSRF-TOKEN",
            url : PluginHelper.getPluginRestUrl('custom-governance') + '/entitlementConfiguration/update',
            headers: {'Content-Type': 'application/json'},
            data : entitlementConfiguration
      }).then(function mySuccess(response) {
          deferred.resolve(response.data);
          console.log(deferred.promise);
            return deferred.promise;
        }, function myError(response) {
            deferred.reject(response);
            return deferred.promise;
        });
    };
    
  });
}());