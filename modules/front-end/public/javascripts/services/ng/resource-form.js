/**
  Provides two directive: remote-resource and resource-property. They handle asynchronous
  live posting and validation of form data with arbitrary remote data sources.

  This file needs some serious dox. Until then, see the checkout page for best usage
*/
/*global angular*/
define(
[
  "ngApp",
  "services/logging",
  "module",
  "services/ng/monitor-user-attention"
],
function(ngApp, logging, module) {
  var log = logging.namespace(module.id);
  var forEach = angular.forEach;
  var noop = angular.noop;

  ngApp.directive("remoteResource", [function() {
    return {
      restrict: 'A',
      scope: true,
      controller: ["$scope", "$http", "$timeout", "$parse", "$attrs", "$interpolate", function($scope, $http, $timeout, $parse, $attrs, $interpolate) {
        var self = this;
        var UrlResourceDelegate = function(url) {
          this.url = url;

          /** Callbacks must have onSubmitEnd, onSubmitSuccess, onSubmitFail */
          this.submit = function(callbacks) {
            var onSubmitEnd = function(data, status, headers) {
              log("URL Resource submission returned with code " + status + ": " + $scope.url);

              var errors = data? data.errors: undefined;
              
              if (status === 200) {
                callbacks.onSubmitSuccess(data);
              } else {
                callbacks.onSubmitFail(data, errors);
              }

              callbacks.onSubmitEnd(data, errors);
            };

            $http.post($scope.url, $scope.resource())
              .success(onSubmitEnd)
              .error(onSubmitEnd);
            
            $scope.form.$submitting = true;
          };

          this.get = function(gotNewResource) {
            $http.get($scope.url).success(function(newResource) {
              gotNewResource(newResource);
            });
          };
        };

        var CustomResourceDelegate = function(getResource, submitResource) {
          this.submit = function(callbacks) {
            submitResource($scope.controls, callbacks);
          };

          this.get = function(gotNewResource) {
            getResource($scope.controls, function(newResource) {
              gotNewResource(newResource);
            });
          };
        };

        $scope.controls = {};
        $scope.submitResource = $parse($attrs.submitResource)($scope.$parent);
        $scope.url = $interpolate($attrs.remoteResource)($scope.$parent);
        $scope.resourceDelegate = new UrlResourceDelegate($scope.url);

        $scope.resource = function() {
          var resource = {};
          forEach($scope.controls, function(formControl, name) {
            resource[name] = formControl.$modelValue;
          });

          return resource;
        };

        this.clearRemoteErrors = function() {
          forEach($scope.controls, function(control) {
            forEach(control.$error, function(isError, errorName) {
              if (errorName.indexOf("remote_") === 0) {
                control.$setValidity(errorName, true);
              }
            });
          });
        };

        $scope.assignResource = function(newResource) {
          $parse($attrs.localResource).assign($scope.$parent, newResource);
        };

        this.getResource = function() {
          $scope.resourceDelegate.get(function(newResource) {
            $scope.assignResource(newResource);
            self.submit();
            forEach(newResource, function(value, name) {
              if ($scope.controls[name]) {
                $scope.controls[name].userAttention.setAttended(true);
              }
            });
          });
        };

        this.setResourceBehavior = function(behavior) {
          var _behavior = angular.extend({
            get: noop,
            submit: noop
          }, behavior);
          
          $scope.resourceDelegate = new CustomResourceDelegate(_behavior.get, _behavior.submit);
        };

        this.addControl = function(formControl) {
          $scope.controls[formControl.$name] = formControl;
          formControl.$setValidity("remote_validated_on_server", false);
        };

        this.submit = function(config) {
          var _config = angular.extend({
            onSubmitEnd: noop,
            onSubmitSuccess: noop,
            onSubmitFail: noop,
            delay: 1000
          }, config);

          var onSubmitEnd = function(data, errors) {
            var affectedFormControl;

            $scope.form.$submitting = false;
            
            forEach($scope.controls, function(inputControl, name) {
              inputControl.setSubmitting(false);
            });

            _config.onSubmitEnd();

            if (errors) {
              forEach(errors, function(error) {
                if (error.field) {
                  affectedFormControl = $scope.controls[error.field];
                  log("Error: " + error.field + " " + error.cause);
                  affectedFormControl.$setValidity("remote_" + error.cause, false);
                }
              });
            }

            if($attrs.submitCompleted) {
              $parse($attrs.submitCompleted)($scope.$parent);
            }

          };

          self.clearRemoteErrors();
          // Cancel any already scheduled submissions
          // TODO: when this issue (https://github.com/angular/angular.js/issues/1159)
          // gets merged into main-line, then also cancel currently active submissions.
          if ($scope.scheduledSubmission) {
            $timeout.cancel($scope.scheduledSubmission);
          }

          // Perform the submission after a few seconds
          $scope.scheduledSubmission = $timeout(function() {
            $scope.resourceDelegate.submit(
              {onSubmitEnd: onSubmitEnd,
               onSubmitSuccess: _config.onSubmitSuccess,
               onSubmitFail: _config.onSubmitFail}
            );
          }, _config.delay);
        };

        $scope.RemoteResourceController = this;
      }],
      
      require: ['form', 'remoteResource'],

      link: function(scope, element, attrs, requisites) {
        var form = requisites[0];
        var remoteResource = requisites[1];

        form.resource = remoteResource;
        
        scope.form = form;
        scope.form.$submitting = false;
        scope.$parent[attrs.name] = form;
        remoteResource.getResource();
      }
    };
  }])

  .directive("resourceProperty", [function() {
    return {
      restrict: 'A',
      scope: true,
      require: ['^remoteResource', 'ngModel', 'monitorUserAttention'],
      controller: function(){},
      link: function(scope, element, attrs, requisites) {
        var remoteResource = requisites[0];
        var inputControl = requisites[1];
        var userAttention = requisites[2];

        var hasLocalErrors = function(inputControl) {
          var foundLocalError = false;
          
          forEach(inputControl.$error, function(isError, errorName) {
            if (errorName.indexOf("remote_") === -1) {
              foundLocalError = true;
            }
          });

          return foundLocalError;
        };

        var submitFormWithDelay = function(delayMs) {
          inputControl.setSubmitting(true);
          remoteResource.submit({
            onSubmitEnd: function() { inputControl.setSubmitting(false); },
            delayMs: delayMs
          });
        };

        inputControl.setSubmitting = function(submitting) {
          inputControl.$submitting = submitting;
          if (submitting) {
            element.addClass("ng-submitting");
          } else {
            element.removeClass("ng-submitting");
          }
        };
        userAttention.listeners.push(function() {
          // Only submit if there are no local errors to take care of.
          // This happens implicitly when submission is triggered by $viewChangeListeners
          // below.
          if (!hasLocalErrors(inputControl) && attrs.noSubmitOnBlur === undefined) {
            submitFormWithDelay(0);
          }
        });
        
        remoteResource.addControl(inputControl);
        inputControl.$viewChangeListeners.push(function() {
          submitFormWithDelay(500);
        });
      }
    };
  }]);
});