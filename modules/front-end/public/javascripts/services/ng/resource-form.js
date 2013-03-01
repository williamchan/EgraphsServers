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
      controller: ["$scope", "$http", "$timeout", "$parse", "$attrs", "$interpolate", function($scope, $http, $timeout, $parse, $attrs, $interpolate) {
        var self = this;

        var UrlResourceDelegate = function(url) {
          this.url = url;

          /** Callbacks must have onSubmitEnd, onSubmitSuccess, onSubmitFail */
          this.submit = function(callbacks) {
            var onSubmitEnd = function(data, status, headers) {
              log("URL Resource submission returned with code " + status + ": " + self.url);

              var errors = data? data.errors: undefined;
              
              if (status === 200) {
                callbacks.onSubmitSuccess(data);
              } else {
                callbacks.onSubmitFail(data, errors);
              }

              callbacks.onSubmitEnd(data, errors);
            };

            $http.post(self.url, self.resource())
              .success(onSubmitEnd)
              .error(onSubmitEnd);
            
            self.form.$submitting = true;
          };

          this.get = function(gotNewResource) {
            $http.get(self.url).success(function(newResource) {
              gotNewResource(newResource);
            });
          };
        };

        var CustomResourceDelegate = function(getResource, submitResource) {
          this.submit = function(callbacks) {
            submitResource(self.controls, callbacks);
          };

          this.get = function(gotNewResource) {
            getResource(self.controls, function(newResource) {
              gotNewResource(newResource);
            });
          };
        };

        self.controls = {};
        self.submitResource = $parse($attrs.submitResource)($scope);
        self.url = $interpolate($attrs.remoteResource)($scope);
        self.resourceDelegate = new UrlResourceDelegate(self.url);

        self.resource = function() {
          var resource = {};
          forEach(self.controls, function(formControl, name) {
            if (formControl.$modelValue) resource[name] = formControl.$modelValue;
          });

          return resource;
        };

        this.clearRemoteErrors = function() {
          var clearControlErrors = function(control) {
            forEach(control.$error, function(isError, errorName) {
              if (errorName.indexOf("remote_") === 0 && isError) {
                control.$setValidity(errorName, true);
              }
            });
          };
          
          forEach(self.controls, clearControlErrors);
          clearControlErrors(self.form);
        };

        self.assignResource = function(newResource) {
          self.isAssigning = true;
          $parse($attrs.localResource).assign($scope, newResource);
          self.isAssigning = false;
        };

        this.getResource = function() {
          self.resourceDelegate.get(function(newResource) {
            self.assignResource(newResource);
            self.clearRemoteErrors();
            self.submit();
            forEach(newResource, function(value, name) {
              if (self.controls[name]) {
                self.controls[name].userAttention.setAttended(true);
              }
            });
          });
        };

        this.setResourceBehavior = function(behavior) {
          var _behavior = angular.extend({
            get: noop,
            submit: noop
          }, behavior);
          
          self.resourceDelegate = new CustomResourceDelegate(_behavior.get, _behavior.submit);
        };

        this.addControl = function(inputControl) {
          self.controls[inputControl.$name] = inputControl;
          inputControl.$setValidity("remote_validated_on_server", false);
        };

        this.setForm = function(form) {
          self.form = form;
          self.form.$controls = self.controls;
        };

        this.submit = function(config) {
          var _config = angular.extend({
            onSubmitEnd: noop,
            onSubmitSuccess: noop,
            onSubmitFail: noop,
            delay: 1000
          }, config);

          var onSubmitEnd = function(data, fieldsAndErrors) {
            var affectedFormControl;

            self.form.$submitting = false;
            
            forEach(self.controls, function(inputControl, name) {
              inputControl.setSubmitting(false);
            });

            _config.onSubmitEnd();

            // Handle errors
            self.clearRemoteErrors();
            if (fieldsAndErrors) {
              log("Errors in submission:");
              forEach(fieldsAndErrors, function(errors, fieldName) {
                log("    \"" + fieldName + "\": " + errors);
                affectedFormControl = self.controls[fieldName];
                forEach(errors, function(error) {
                  if (affectedFormControl) {
                    affectedFormControl.$setValidity("remote_" + error, false);
                  }
                });
              });
            }

            // Any submit complete callback
            if($attrs.submitCompleted) {
              $parse($attrs.submitCompleted)($scope);
            }
          };

          // Cancel any already scheduled submissions
          // TODO: when this issue (https://github.com/angular/angular.js/issues/1159)
          // gets merged into main-line, then also cancel currently active submissions.
          if (self.scheduledSubmission) {
            $timeout.cancel(self.scheduledSubmission);
          }

          self.form.$submitting = true;
          // Perform the submission after a few seconds
          self.scheduledSubmission = $timeout(function() {
            self.resourceDelegate.submit(
              {onSubmitEnd: onSubmitEnd,
               onSubmitSuccess: _config.onSubmitSuccess,
               onSubmitFail: _config.onSubmitFail}
            );
          }, _config.delay);
        };
      }],
      
      require: ['form', 'remoteResource'],

      link: function(scope, element, attrs, requisites) {
        var form = requisites[0];
        var remoteResource = requisites[1];

        form.resource = remoteResource;
        
        remoteResource.setForm(form);
        remoteResource.form.$submitting = false;
        
        remoteResource.getResource();
      }
    };
  }])

  .directive("resourceProperty", [function() {
    return {
      restrict: 'A',
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