/* Configures angular to send form url encoded requests rather than JSON requests by default */
define(
[
  "ngApp"
],
function(ngApp) {
  ngApp.run(["$http", function($http) {
    $http.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded;charset=utf-8';
    $http.defaults.transformRequest = [function(data) {
      if (typeof(data) === 'object') {
        return $.param(data);
      } else {
        return data;
      }
    }];
  }]);
});