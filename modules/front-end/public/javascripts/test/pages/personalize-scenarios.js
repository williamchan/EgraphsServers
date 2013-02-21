/* Testing scenarios for the personalize page in the purchase flow */
/*global angular, describe, it*/
define(
[
  "test/mock-backend",
  "services/logging",
  "module",
  "ngApp"
  ],
function(mockBackend, logging, module, ngApp) {
  var log = logging.namespace(module.id);
  var idSequence = 1;
  var BAD_REQUEST = 400;
  var mockApi = {};

  var stubApi = function(path, propName) {
    mockBackend.stubResource({
      path: path,
      get: function(data) {
        if(mockApi[propName] !== undefined) {
          return [200, mockApi[propName], {}];
        } else {
          return [404, "", {}];
        }
        
      },
      post: function(data) {
        mockApi[propName] = data;
        return [200, "", {}];
      }
    });
  };

  var fieldErrors = function(fieldName, errors) {
    var errorApiObjects = [];
    
    angular.forEach(errors, function(error) {
      errorApiObjects.push({field:fieldName, cause:error});
    });

    return errorApiObjects;
  };

  var configureDefaultApi = function() {
    stubApi(/egraph/, 'egraph');
  };

  return {
    "default": {
      bootstrap: function() {
        configureDefaultApi();
      }
    }
  };
});