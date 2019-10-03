(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp');

  app.service('GovernancePluginService', function($http, $q) {
    /*
     * return all template names found in the system
     */
    this.getConfiguration = function() {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url : PluginHelper.getPluginRestUrl('custom-governance') + '/getGovernanceModel'
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

    /*
     * get the input parameters of a template
     */
    this.getAvailableRules = function(templateName) {
      var deferred = $q.defer();
      return $http({
        method: "GET",
        withCredentials: true,
        xsrfHeaderName: "X-XSRF-TOKEN",
        xsrfCookieName: "CSRF-TOKEN",
        url: PluginHelper.getPluginRestUrl('custom-governance') + '/getRuleNames'
      }).then(function mySuccess(response) {
        deferred.resolve(response.data);
        return deferred.promise;
      }, function myError(response) {
        deferred.reject(response);
        return deferred.promise;
      });
    };

    /*
     * evaluate a template with an argument array
     */
    /*
        this.evaluateTemplate = function(templateName, argumentArray) {
        	var deferred  = $q.defer(); 
        	return $http({
        		method  : "POST",
                withCredentials: true,
                xsrfHeaderName : "X-XSRF-TOKEN",
                xsrfCookieName : "CSRF-TOKEN",
                url : PluginHelper.getPluginRestUrl('velocity-template') + '/evaluateTemplate',
                headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                transformRequest: function(obj) {
                    var str = [];
                    for(var p in obj) {
                    	str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                    }  
                    return str.join("&");
                },
                data : {
                	templateName : templateName,
                    arguments 	 : angular.toJson(argumentArray)
                }
        	}).then(function mySuccess(response) {
            	deferred.resolve(response.data);
                return deferred.promise;
            }, function myError(response) {
                deferred.reject(response);
                return deferred.promise;
            });
        };
        */
  });
}());