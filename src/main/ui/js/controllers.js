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

      this.$onInit = function () {
        $scope.isAdmin();
      };

    }
  ]);

  app.controller('HomeController', ['$scope', '$http', '$uibModal', '$timeout', 'GovernancePluginService',
    function($scope, $http, $uibModal, $timeout, GovernancePluginService) {
      $scope.configObject = {};

      var controller = this;

      $scope.rules                  = [];
      $scope.infoMessage            = null;
      $scope.successMessage         = null;
      $scope.newApproverName        = null;
      
      $scope.toggleIndicators = {
        "showApplyChangesButton"  : false,
        "showApprovalLevels"      : false,
        "showApprovalRules"       : false,
        "showAddNewApprover"      : false,
        "showInfoMessage"         : false,
        "showSuccessMessage"      : false
      };

      controller.getGovernanceModel = function() {
        GovernancePluginService.getGovernanceModel().then(function(result) {
          $scope.configObject = result;
          $scope.approvalLevelArray = Object.keys($scope.configObject.approvalLevels).map(function(key) {
            return key;
          });
        });
      };

      controller.getAvailableRules = function() {
        GovernancePluginService.getAvailableRules("all").then(function(result) {
          $scope.rules = result;
        });
      };

      controller.toggleView = function(functionName) {
        if (functionName === 'showApprovalLevels') {
          $scope.toggleIndicators.showApprovalLevels = !$scope.toggleIndicators.showApprovalLevels;
        }
        if (functionName === 'showApprovalRules') {
          $scope.toggleIndicators.showApprovalRules = !$scope.toggleIndicators.showApprovalRules;
        }
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
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/approvals/approvalLevelModal.html'),
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
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/approvals/approverLookupModal.html'),
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
        GovernancePluginService.saveGovernanceModel($scope.configObject).then(function(result) {
          // success getting the setup information
          $scope.toggleIndicators.showApplyChangesButton = false;
          $scope.toggleIndicators.showApprovalLevels     = false;
          $scope.toggleIndicators.showApprovalRules      = false;
          controller.toggleShowSuccessMessage("Model successfully saved");
        }, function(result) {
          // something went wrong getting the setup information
          controller.toggleShowErrorMessage(result.data);
        });

      };

      controller.revertChanges = function() {
        controller.getGovernanceModel();
        $scope.toggleIndicators.showApprovalLevels     = false;
        $scope.toggleIndicators.showApprovalRules      = false;
        $scope.toggleIndicators.showApplyChangesButton = false;
        controller.toggleShowSuccessMessage("Model successfully reverted");
      };

      controller.deleteApproverLookup = function(approver) {
        $scope.toggleIndicators.showApplyChangesButton = true;
        delete $scope.configObject.approverLookupRules[approver];
      };

      controller.deleteApprovalLevel = function(approvalLevel) {
        $scope.toggleIndicators.showApplyChangesButton = true;
        delete $scope.configObject.approvalLevels[approvalLevel];
        $scope.approvalLevelArray = Object.keys($scope.configObject.approvalLevels).map(function(key) {
          return key;
        });
      };

      /* Events from Modals */
      $scope.$on('saveApprovalEvent', function(event, args) {
        var name = args.name;
        var originalName = args.originalName;
        var approval = [];
        var notification = [];
        approval = [...args.approval];
        notification = [...args.notification];

        if (name === originalName || !GovernancePluginService.isValueInArray($scope.approvalLevelArray, originalName)) {
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

        $scope.toggleIndicators.showApplyChangesButton = true;

        $scope.approvalLevelArray = Object.keys($scope.configObject.approvalLevels).map(function(key) {
          return key;
        });
      });

      $scope.$on('saveApproverLookupEvent', function(event, args) {
        var approverName = args.approverName;
        var originalName = args.originalName;
        var ruleName = args.ruleName;

        if (approverName === originalName ||
          !GovernancePluginService.isValueInArray(Object.keys($scope.configObject.approverLookupRules), originalName)) {
          // simple push
          $scope.configObject.approverLookupRules[approverName] = ruleName;
        } else {
          delete $scope.configObject.approverLookupRules[originalName];
          $scope.configObject.approverLookupRules[approverName] = ruleName;
        }

        $scope.toggleIndicators.showApplyChangesButton = true;
      });

      this.$onInit = function () {
        controller.getGovernanceModel();
        controller.getAvailableRules();
      }
    }
  ]);

  app.controller('SetupController', ['$scope', '$http', '$uibModal', '$timeout', 'GovernancePluginService',
    function($scope, $http, $uibModal, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.infoMessage        = null;
      $scope.successMessage     = null;
      $scope.errorMessage       = null;

      $scope.toggleIndicators   = {
          "showInfoMessage"   : false,
          "successMessage"    : false,
          "showErrorMessage"  : false
      };
      
      $scope.setupInformation = {
          workflow : "",
          steps: [],
          integration : true,
          userAgreement : false,
          tasks : [],
          aggregationRule : null
      };

      $scope.groupAggregationtasks = null;

      $scope.modeInfo = {
          "parallel" : {
            description: "Approvals are processed concurrently and there must be consensus, we wait for all approvers to approve.  The first approver that rejects terminates the entire approval."
          },
          "parallelPoll" : {
            description: "Approvals are processed concurrently but consensus is not required. All approvals will be processed, we don't stop if there are any rejections."
          },
          "serial" : {
            description: "Approvals are processed one at a time and there must be consensus. The first approver that rejects terminates the entire approval."
          },
          "serialPoll" : {
            description: "Approvals are processed in order but consensus is not required. All approvals will be processed, we don't stop if there are any rejections.  In effect we are 'taking a poll' of the approvers."
          },
          "any" : {
            description: "Approvals are processed concurrently, the first approver to respond makes the decision for the group."
          }
      }

      $scope.approvalModes = Object.keys($scope.modeInfo);

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

      controller.getSetupInformation = function() {
        GovernancePluginService.getSetupInformation().then(function(result) {
          // success getting the setup information
          $scope.setupInformation = result;
          $scope.groupAggregationtasks = [...result.tasks];
        }, function(result) {
          // something went wrong getting the setup information
          $scope.toggleIndicators.showErrorMessage   = true;
          $scope.errorMessage       = result.data;
        });
      };

      controller.setupSystemIntegration = function() {
        GovernancePluginService.performIntegration($scope.setupInformation).then(function(result) {
          controller.toggleShowSuccessMessage("system successfully set up.");
          $scope.setupInformation.integration = true;
        }, function(result) {
          // something went wrong getting the setup information
          $scope.toggleIndicators.showErrorMessage   = true;
          $scope.errorMessage       = result.data;
        });
      };

      controller.revertSystemIntegrationStatus = function() {
        GovernancePluginService.revertIntegrationStatus().then(function(result) {
          $scope.setupInformation.integration = false;
        });
      };

      this.$onInit = function () {
        controller.getSetupInformation();
      }
    }
  ]);

  app.controller('EntitlementController', ['$scope', '$http', '$uibModal', '$timeout', 'GovernancePluginService',
    function($scope, $http, $uibModal, $timeout, GovernancePluginService) {
      var controller = this;

      $scope.infoMessage          = null;
      $scope.successMessage       = null;
      $scope.errorMessage         = null;
      
      $scope.selectionTypes       = ["regex",  "rule"];
      $scope.ownerSelectionTypes  = ["static", "rule"];
      $scope.ruleNames            = [];
      $scope.groupRefreshRules    = [];
      $scope.afterRuleNames       = [];
      $scope.applicationNames     = [];
      $scope.approvalLevels       = [];
      $scope.configObject         = {};
      
      
      $scope.toggleIndicators = {
        "showApplicationGeneralConfiguration"     : {},
        "showApplicationEntitlementConfiguration" : {},
        "showApplication"     : {},
        "showDescriptor"      : {},
        "showInfoMessage"     : false,
        "showSuccessMessage"  : false,
        "showErrorMessage"    : false
      };

      controller.logObject = function() {
        console.log($scope.configObject);
      };

      controller.getAvailableRules = function() {
        GovernancePluginService.getAvailableRules("all").then(function(result) {
          $scope.ruleNames = result;
        });
      };

      controller.getApprovalLevels = function() {
        GovernancePluginService.getApprovalLevels().then(function(result) {
          $scope.approvalLevels = result;
        });
      };

      controller.getGroupRefreshRules = function() {
        GovernancePluginService.getAvailableRules("GroupAggregationRefresh").then(function(result) {
          $scope.groupRefreshRules = result;
        });
      };

      controller.getAvailableApplications = function() {
        GovernancePluginService.getAvailableApplications().then(function(result) {
          $scope.applicationNames = result;
        });
      };

      controller.getEntitlementConfiguration = function() {
        GovernancePluginService.getEntitlementConfiguration().then(function(result) {
          $scope.configObject = result;
        });
      };

      controller.closeAllToggles = function() {
        for(var key in $scope.toggleIndicators.showApplication) {
          $scope.toggleIndicators.showApplication[key] = false;
        }

        for(var key in $scope.toggleIndicators.showDescriptor) {
          $scope.toggleIndicators.showDescriptor[key] = false;
        }
      };

      controller.toggleView = function(name, descriptor) {
        if(typeof descriptor === 'undefined') {
          var currentSetting = $scope.toggleIndicators.showApplication[name];
          if(typeof currentSetting === 'undefined') {
            currentSetting = false;
          }

          $scope.toggleIndicators.showApplication[name] = !currentSetting;
        } else {
          if(typeof $scope.toggleIndicators.showDescriptor[name] === 'undefined') {
            $scope.toggleIndicators.showDescriptor[name] = {};
          }

          if(typeof $scope.toggleIndicators.showDescriptor[name][descriptor] === 'undefined') {
            $scope.toggleIndicators.showDescriptor[name][descriptor] = false;
          }

          $scope.toggleIndicators.showDescriptor[name][descriptor] = !$scope.toggleIndicators.showDescriptor[name][descriptor];
        }
      };
      
      controller.toggleAppGeneralView = function(name) {

        var currentSetting = $scope.toggleIndicators.showApplicationGeneralConfiguration[name];
        if(typeof currentSetting === 'undefined') {
          currentSetting = false;
        }

        $scope.toggleIndicators.showApplicationGeneralConfiguration[name] = !currentSetting;
      
      };
      
      controller.toggleAppEntitlementView = function(name) {
        console.log(name);
        var currentSetting = $scope.toggleIndicators.showApplicationEntitlementConfiguration[name];
        if(typeof currentSetting === 'undefined') {
          currentSetting = false;
        }

        $scope.toggleIndicators.showApplicationEntitlementConfiguration[name] = !currentSetting;
      
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

      controller.toggleShowErrorMessage = function(message) {
        $scope.errorMessage = message;
        $scope.toggleIndicators.showErrorMessage = true;
        $timeout(function() {
          $scope.errorMessage = null;
          $scope.toggleIndicators.showErrorMessage = false;
        }, 3000);
      };

      controller.deleteApplication = function(name) {
        // wtf moment. For any reason, when trying to delete in this function
        // I always got the wrong JSON object?!?
        // working around by emitting an event
        $scope.$emit('deleteApplicationEvent', name);
      };

      controller.deleteEntitlementConfiguration = function(object, index) {
        object.splice(index, 1);
      };

      controller.raise = function(object, index) {
        GovernancePluginService.raisePositionInArray(object, index);
      };

      controller.lower = function(object, index) {
        GovernancePluginService.lowerPositionInArray(object, index);
      };

      controller.saveEntitlementConfiguration = function() {
        GovernancePluginService.saveEntitlementConfiguration($scope.configObject).then(function(result) {
          // success getting the setup information
          controller.closeAllToggles();
          controller.toggleShowSuccessMessage("Entitlement Configuration sucessfully saved");
        }, function(result) {
          // something went wrong getting the setup information
          controller.toggleShowErrorMessage(result.data);
        });

      }

      controller.addApplication = function() {
        $scope.addApplicationModal($scope, $uibModal);
      };

      $scope.addApplicationModal = function($scope, $uibModal) {
        var modalScope = $scope.$new();
        var modalInstance = $uibModal.open({
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/entitlements/addApplicationModal.html'),
          controller: 'AddApplicationModalController',
          windowClass: 'app-modal-window',
          scope: modalScope
        });

        var usedApplicationNames = [];
        if($scope.configObject.ApplicationConfiguration !== null) {
          usedApplicationNames = Object.keys($scope.configObject.ApplicationConfiguration);
        }

        var filteredApplicationNames  = GovernancePluginService.getArrayExceptValues($scope.applicationNames, usedApplicationNames);

        modalScope.modalInstance      = modalInstance;
        //TODO: remove already created application names from list
        modalScope.applicationNames   = filteredApplicationNames;
        modalScope.approvalLevels     = $scope.approvalLevels;
        modalScope.groupRefreshRules  = $scope.groupRefreshRules;
        modalScope.ruleNames          = $scope.ruleNames;

      };

      controller.revertChanges = function() {
        GovernancePluginService.getEntitlementConfiguration().then(function(result) {
          // success getting the setup information
          $scope.configObject = result;
          controller.closeAllToggles();
          controller.toggleShowSuccessMessage("Model successfully reverted");
        }, function(result) {
          // something went wrong getting the setup information
          controller.toggleShowErrorMessage(result.data);
        });
      };

      controller.addEntitlementConfiguration = function(applicationName) {
        $scope.addEntitlementConfigurationModal(applicationName, $scope, $uibModal);
      };

      $scope.addEntitlementConfigurationModal = function(applicationName, $scope, $uibModal) {
        var modalScope = $scope.$new();
        var modalInstance = $uibModal.open({
          templateUrl: PluginHelper.getPluginFileUrl('custom_governance_plugin', 'ui/modals/entitlements/addEntitlementConfigurationModal.html'),
          controller: 'AddApplicationModalController',
          windowClass: 'app-modal-window',
          scope: modalScope
        });

        var usedApplicationNames = [];
        if($scope.configObject.ApplicationConfiguration !== null) {
          usedApplicationNames = Object.keys($scope.configObject.ApplicationConfiguration);
        }
        var filteredApplicationNames    = GovernancePluginService.getArrayExceptValues($scope.applicationNames, usedApplicationNames);

        modalScope.applicationName      = applicationName;
        modalScope.modalInstance        = modalInstance;
        modalScope.applicationNames     = filteredApplicationNames;
        modalScope.approvalLevels       = $scope.approvalLevels;
        modalScope.ruleNames            = $scope.ruleNames;
        modalScope.selectionTypes       = $scope.selectionTypes;
        modalScope.ownerSelectionTypes  = $scope.ownerSelectionTypes;
        modalScope.groupRefreshRules    = $scope.groupRefreshRules;

      };

      $scope.$on('addApplicationEvent', function(event, args) {
        var applicationName       = args.applicationName;
        var defaultApprovalLevel  = args.defaultApprovalLevel;
        var defaultOwner          = args.defaultOwner;
        var isRequestable         = args.isRequestable;
        var afterRuleName         = args.afterRuleName;
        var runAfterRule          = args.runAfterRule;

        var object = {
          "EntitlementConfiguration" : [],
          "GeneralConfiguration" : {
            "defaultApprovalLevel": defaultApprovalLevel,
            "defaultOwner"        : defaultOwner,
            "isRequestable"       : isRequestable,
            "afterRuleName"       : afterRuleName,
            "runAfterRule"        : runAfterRule
          }
        };

        if($scope.configObject.ApplicationConfiguration === null) {
          $scope.configObject.ApplicationConfiguration = {};
        }

        $scope.configObject.ApplicationConfiguration[applicationName] = object;
      });

      $scope.$on('deleteApplicationEvent', function(event, args) {
        var applicationName = args;

        delete $scope.configObject.ApplicationConfiguration[applicationName];
      });

      $scope.$on('addEntitlementConfigurationEvent', function(event, args) {
        var applicationName = args.appName;
        var configuration   = args.config;

        if($scope.configObject.ApplicationConfiguration !== null) {
          if($scope.configObject.ApplicationConfiguration[applicationName].EntitlementConfiguration == null) {
            $scope.configObject.ApplicationConfiguration[applicationName].EntitlementConfiguration = [];
          }
          var list            = $scope.configObject.ApplicationConfiguration[applicationName].EntitlementConfiguration;

          if (list != null && !list.some(e => e.descriptor === configuration.descriptor)) {
            /* list contains the element we're looking for */
            $scope.configObject.ApplicationConfiguration[applicationName].EntitlementConfiguration.push(configuration);
          }
        }
      });

      this.$onInit = function () {
        controller.getAvailableRules();
        controller.getGroupRefreshRules();
        controller.getAvailableApplications();
        controller.getEntitlementConfiguration();
        controller.getApprovalLevels();
      };
    }
  ]);
}());