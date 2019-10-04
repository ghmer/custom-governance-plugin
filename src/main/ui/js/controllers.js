(function() {
  'use strict';

  var app = angular.module('customGovernancePluginApp');

  /** HOME Controller **/
  app.controller('NavigateController', ['$scope', '$http', '$timeout',
    function($scope, $http, $timeout) {
      $scope.admin = false;
      $scope.isAdmin = function() {
        $http({
          method: "GET",
          withCredentials: true,
          xsrfHeaderName: "X-XSRF-TOKEN",
          xsrfCookieName: "CSRF-TOKEN",
          url: PluginHelper.getPluginRestUrl('custom-governance') + '/isAdmin'
        }).then(function mySuccess(response) {
          if (response.data === true) {
            $scope.admin = true;
          }
        }, function myError(response) {
          $scope.admin = false;
        });
      };

      try {
        $scope.isAdmin();
      } catch (error) {
        console.log("could not determine whether or not we are an admin");
      }

    }
  ]);

  app.controller('HomeController', ['$scope', '$http', '$uibModal', '$timeout', 'GovernancePluginService',
    function($scope, $http, $uibModal, $timeout, GovernancePluginService) {
      $scope.headline = 'Hello, Stranger!';
      $scope.configObject = {};

      var controller = this;

      $scope.showApplyChangesButton = false;
      $scope.showApprovalLevels = false;
      $scope.showApprovalRules = false;
      $scope.showAddNewApprover = false;
      $scope.rules = [];
      $scope.infoMessage = null;
      $scope.showInfoMessage = false;
      $scope.newApproverName = null;

      controller.getGovernanceModel = function() {
        GovernancePluginService.getGovernanceModel().then(function(result) {
          $scope.configObject = result;
          $scope.approvalLevelArray = Object.keys($scope.configObject.approvalLevels).map(function(key) {
            return key;
          });
        });
      };

      controller.getAvailableRules = function() {
        GovernancePluginService.getAvailableRules().then(function(result) {
          $scope.rules = result;
        });
      };

      controller.toggleView = function(functionName) {
        if (functionName === 'showApprovalLevels') {
          $scope.showApprovalLevels = !$scope.showApprovalLevels;
        }
        if (functionName === 'showApprovalRules') {
          $scope.showApprovalRules = !$scope.showApprovalRules;
        }
      };

      controller.toggleShowInfoMessage = function(message) {
        $scope.infoMessage = message;
        $scope.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.showInfoMessage = false;
        }, 3000);
      };

      controller.showAddApprovalLevelModal = function() {
        controller.approvalLevelModal(null,
          ["none"],
          [],
          Object.keys($scope.configObject.approverLookupRules),
          $scope.approvalLevelArray,
          $scope,
          $uibModal);
      };

      controller.showApprovalLevelModal = function(approvalLevel) {
        var approvalLevelName = approvalLevel;
        var approvalConfig = $scope.configObject.approvalLevels[approvalLevel].approval;
        var notificationConfig = $scope.configObject.approvalLevels[approvalLevel].notification;

        controller.approvalLevelModal(approvalLevelName,
          approvalConfig,
          notificationConfig,
          Object.keys($scope.configObject.approverLookupRules),
          GovernancePluginService.getArrayExceptValue(approvalLevelName, $scope.approvalLevelArray),
          $scope,
          $uibModal);
      };

      controller.approvalLevelModal = function(approvalLevelName, approvalConfig,
        notificationConfig, availableApprovers, usedApprovalLevelNames, $scope, $uibModal) {
        var modalScope = $scope.$new();
        var modalInstance = $uibModal.open({
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/approvalLevelModal.html'),
          controller: 'ApprovalLevelModalController',
          windowClass: 'app-modal-window',
          scope: modalScope
        });

        modalScope.approvers = availableApprovers;
        modalScope.approvalName = approvalLevelName;
        modalScope.approvalConfig = approvalConfig;
        modalScope.usedNames = usedApprovalLevelNames;
        modalScope.notificationConfig = notificationConfig;
        modalScope.modalInstance = modalInstance;
      };

      controller.showAddNewApproverLookupModal = function() {
        controller.approverLookupModal(null,
          Object.keys($scope.configObject.approverLookupRules),
          null,
          $scope.rules,
          $scope,
          $uibModal);
      };

      controller.showApproverLookupModal = function(approverName, ruleName) {
        var list = Object.keys($scope.configObject.approverLookupRules);
        controller.approverLookupModal(approverName,
          GovernancePluginService.getArrayExceptValue(list, approverName),
          ruleName,
          $scope.rules,
          $scope,
          $uibModal);
      };

      controller.approverLookupModal = function(approverName, approverNames, ruleName, rules, $scope, $uibModal) {
        var modalScope = $scope.$new();
        var modalInstance = $uibModal.open({
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/approverLookupModal.html'),
          controller: 'ApproverLookupModalController',
          windowClass: 'app-modal-window',
          scope: modalScope
        });

        modalScope.approvers = approverNames;
        modalScope.rules = rules;
        modalScope.ruleName = ruleName;
        modalScope.approverName = approverName;
        modalScope.modalInstance = modalInstance;
      };

      controller.applyChanges = function() {
        GovernancePluginService.saveGovernanceModel($scope.configObject);
        $scope.showApplyChangesButton = false;
      };

      controller.revertChanges = function() {
        controller.getGovernanceModel();
        $scope.showApplyChangesButton = false;
      };

      /* Events from Modals */
      $scope.$on('saveApprovalEvent', function(event, args) {
        var name = args.name;
        var originalName = args.originalName;
        var approval = [];
        var notification = [];
        approval = [...args.approval];
        notification = [...args.notification];

        if (name === originalName || !GovernancePluginService.isInList($scope.approvalLevelArray, originalName)) {
          // simple push
          $scope.configObject.approvalLevels[name] = {
            notification: [...notification],
            approval: [...approval]
          };
        } else {
          delete $scope.configObject.approvalLevels[originalName];
          $scope.configObject.approvalLevels[name] = {
            notification: [...notification],
            approval: [...approval]
          };
        }

        $scope.showApplyChangesButton = true;
        $scope.approvalLevelArray = Object.keys($scope.configObject.approvalLevels).map(function(key) {
          return key;
        });
      });

      $scope.$on('saveApproverLookupEvent', function(event, args) {
        var approverName = args.approverName;
        var originalName = args.originalName;
        var ruleName = args.ruleName;

        if (approverName === originalName ||
          !GovernancePluginService.isInList(Object.keys($scope.configObject.approverLookupRules), originalName)) {
          // simple push
          $scope.configObject.approverLookupRules[approverName] = ruleName;
        } else {
          delete $scope.configObject.approverLookupRules[originalName];
          $scope.configObject.approverLookupRules[approverName] = ruleName;
        }

        $scope.showApplyChangesButton = true;
      });

      controller.getGovernanceModel();
      controller.getAvailableRules();

    }
  ]);
  
  app.controller('SetupController', ['$scope', '$http', '$uibModal', '$timeout', 'GovernancePluginService',
    function($scope, $http, $uibModal, $timeout, GovernancePluginService) {
      $scope.headline = 'Hello, Stranger!';
      
      var controller = this;
      
      $scope.infoMessage      = null;
      $scope.showInfoMessage  = false;
      $scope.setupInformation = {};

      controller.getSetupInformation = function() {
        GovernancePluginService.getSetupInformation().then(function(result) {
          $scope.setupInformation = result;
        });
      };

    }
  ]);

  /** ApprovalLevelModalController **/
  app.controller('ApprovalLevelModalController', ['$scope', '$http', '$timeout', 'GovernancePluginService',
    function($scope, $http, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.infoMessage = null;
      $scope.showInfoMessage = false;
      $scope.showAddApprover = false;
      $scope.newApprover = null;
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
        $scope.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.showInfoMessage = false;
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
        $scope.showAddApprover = !$scope.showAddApprover;
      };

      controller.deleteApprover = function(index) {
        $scope.user.approval.splice(index, 1);
        if ($scope.user.approval.length <= 0) {
          $scope.user.approval.push("none");
        }
      };

      controller.raise = function(index) {
        var temp1 = $scope.user.approval[index - 1];
        var temp2 = $scope.user.approval[index];
        $scope.user.approval[index - 1] = temp2;
        $scope.user.approval[index] = temp1;
      };

      controller.lower = function(index) {
        var temp1 = $scope.user.approval[index + 1];
        var temp2 = $scope.user.approval[index];
        $scope.user.approval[index + 1] = temp2;
        $scope.user.approval[index] = temp1;
      };

      controller.saveApprovalLevel = function() {
        if (GovernancePluginService.validateNotInList($scope.usedNames, $scope.user.name)) {
          if ($scope.user.name !== null) {
            if ($scope.approvalLevelForm.$valid) {
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
      $scope.showInfoMessage = false;
      $scope.showAddApprover = false;
      $scope.newApprover = null;
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
        if (GovernancePluginService.validateNotInList($scope.approvers, $scope.user.approverName)) {
          if ($scope.user.approverName !== null && $scope.user.ruleName !== null) {
            if ($scope.approverLookupForm.$valid) {
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
        $scope.showInfoMessage = true;
        $timeout(function() {
          $scope.infoMessage = null;
          $scope.showInfoMessage = false;
        }, 3000);
      };

    }
  ]);
}());