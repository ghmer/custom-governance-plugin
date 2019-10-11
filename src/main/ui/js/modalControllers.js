(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp');
  
  /** ApprovalLevelModalController **/
  app.controller('ApprovalLevelModalController', ['$scope', '$http', '$timeout', 'GovernancePluginService',
    function($scope, $http, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.infoMessage        = null;
      $scope.successMessage     = null;
      $scope.newApprover        = null;
      
      $scope.toggleIndicators = {
        "showInfoMessage"     : false,
        "showSuccessMessage"  : false,
        "showAddApprover"     : false
      };
      
      $scope.btnMessages = {
        "addApprover": {
          "add": "add approver",
          "cancel": "cancel"
        }
      };

      $scope.user = {
        name: $scope.approvalName,
        originalName: $scope.approvalName,
        approval: [...$scope.approvalConfig],
        notification: [...$scope.notificationConfig]
      };

      $scope.approvalLevel = {
        notification: ["user", "manager", "requester", "securityOfficer"]
      };

      controller.toggleShowInfoMessage = function(message) {
        $scope.infoMessage = message;
        $scope.toggleIndicators.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.toggleIndicators.showInfoMessage = false;
        }, 3000);
      };

      controller.toggleShowSuccessMessage = function(message) {
        $scope.successMessage = message;
        $scope.toggleIndicators.showSuccessMessage = true;
        $timeout(function() {
          $scope.successMessage = null;
          $scope.toggleIndicators.showSuccessMessage = false;
        }, 3000);
      };

      controller.addApprover = function() {
        if ($scope.newApprover === "none" && $scope.user.approval.length >= 1) {
          controller.toggleShowInfoMessage("Cannot add none as there are other approvers defined");
        } else {
          if ($scope.user.approval.includes($scope.newApprover)) {
            controller.toggleShowInfoMessage("This entry already belongs to the list of approvers");
          } else {
            if ($scope.user.approval.length === 1) {
              if ($scope.user.approval[0] === "none") {
                $scope.user.approval.splice(0, 1);
              }
            }
            $scope.user.approval.push($scope.newApprover);
            $scope.newApprover = null;
            controller.toggleShowAddApprover();
          }
        }
      };

      controller.toggleShowAddApprover = function() {
        $scope.toggleIndicators.showAddApprover = !$scope.toggleIndicators.showAddApprover;
      };

      controller.deleteApprover = function(index) {
        $scope.user.approval.splice(index, 1);
        if ($scope.user.approval.length <= 0) {
          $scope.user.approval.push("none");
        }
      };

      controller.raise = function(index) {
        GovernancePluginService.raisePositionInArray($scope.user.approval, index);
      };

      controller.lower = function(index) {
        GovernancePluginService.lowerPositionInArray($scope.user.approval, index);
      };

      controller.saveApprovalLevel = function() {
        if(!(GovernancePluginService.isValueInArray($scope.usedNames, $scope.user.name))) {
          if($scope.user.name !== null) {
            if($scope.approvalLevelForm.$valid) {
              $scope.$emit('saveApprovalEvent', $scope.user);
              $scope.modalInstance.close('close');
            }
          }
        } else {
          // is in list
          controller.toggleShowInfoMessage("This name is already being used. Please chose another one");
        }
      };

      controller.close = function() {
        $scope.modalInstance.close('close');
      };

      controller.cancel = function() {
        $scope.modalInstance.dismiss('cancel');
      };
    }
  ]);

  /** ApproverLookupModalController **/
  app.controller('ApproverLookupModalController', ['$scope', '$http', '$timeout', 'GovernancePluginService',
    function($scope, $http, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.infoMessage = null;
      $scope.newApprover = null;
      
      $scope.toggleIndicators = {
        "showInfoMessage"     : false,
        "showSuccessMessage"  : false,
        "showAddApprover"     : false  
      };
      $scope.btnMessages = {
        "addApprover": {
          "add": "add approver",
          "cancel": "cancel"
        }
      };

      $scope.user = {
        approverName: $scope.approverName,
        originalName: $scope.approverName,
        ruleName: $scope.ruleName
      };

      controller.saveApproverLookup = function() {
        if(!(GovernancePluginService.isValueInArray($scope.approvers, $scope.user.approverName))) {
          if($scope.user.approverName !== null && $scope.user.ruleName !== null) {
            if($scope.approverLookupForm.$valid) {
              $scope.$emit('saveApproverLookupEvent', $scope.user);
              $scope.modalInstance.close('close');
            }
          }
        } else {
          // is in list
          controller.toggleShowInfoMessage("This name is already being used. Please chose another one");
        }
      };

      controller.close = function() {
        $scope.modalInstance.close('close');
      };

      controller.cancel = function() {
        $scope.modalInstance.dismiss('cancel');
      };

      controller.toggleShowInfoMessage = function(message) {
        $scope.infoMessage = message;
        $scope.toggleIndicators.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.toggleIndicators.showInfoMessage = false;
        }, 3000);
      };

    }
  ]);
  
  /** ApproverLookupModalController **/
  app.controller('AddApplicationModalController', ['$scope', '$http', '$timeout', 'GovernancePluginService',
    function($scope, $http, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.object = {
          "runAfterRule" : false
      };

      controller.close = function() {
        $scope.modalInstance.close('close');
      };

      controller.cancel = function() {
        $scope.modalInstance.dismiss('cancel');
      };

      controller.addApplicationDefinition = function() {
        $scope.$emit('addApplicationEvent', $scope.object);
        $scope.modalInstance.close('close');
      };

      controller.toggleShowInfoMessage = function(message) {
        $scope.infoMessage = message;
        $scope.toggleIndicators.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.toggleIndicators.showInfoMessage = false;
        }, 3000);
      };

    }
  ]);

  app.controller('AddEntitlementConfigurationModalController', ['$scope', '$http', '$timeout', 'GovernancePluginService',
    function($scope, $http, $timeout, GovernancePluginService) {
      var controller = this;
      $scope.entitlementConfig = {
                  "ownerSelectionType": "static",
                  "selectionType": "regex",
      };
  
      controller.close = function() {
        $scope.modalInstance.close('close');
      };
  
      controller.cancel = function() {
        $scope.modalInstance.dismiss('cancel');
      };
  
      controller.addEntitlementConfiguration = function() {
        var args = {
          "config" : $scope.entitlementConfig,
          "appName": $scope.applicationName
        };
        $scope.$emit('addEntitlementConfigurationEvent', args);
        $scope.modalInstance.close('close');
      };
  
      controller.toggleShowInfoMessage = function(message) {
        $scope.infoMessage = message;
        $scope.toggleIndicators.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.toggleIndicators.showInfoMessage = false;
        }, 3000);
      };
    }
  ]);
  
}());