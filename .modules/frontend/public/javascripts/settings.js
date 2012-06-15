/* Scripting for the settings page */
define([], function() {

  return {
    /**
     * Executes all the scripts for the settings template.

     * @return nothing
     */
    go: function () {
      //Bindings for edit buttons
      $(document).ready(function(){

        $(".edit").click(function(e){
          var containingRow = $(this).parent();

          containingRow.addClass('hide');
          containingRow.next().removeClass('hide');
          e.preventDefault();
        });

      });
    },
          //Controller class for Angular app
    Controller: function ($scope) {
      //Plugin default values here
      $scope.master = { fullname : "Joshua Johnson",
                        username : "joshuajohnson",
                        email : "joshua12@gmail.com",
                        password: "********",
                        address :
                          { title : "Default",
                            fullname : "Joshua Johnson",
                            line1 : "5507 N 58th Street",
                            line2 : "",
                            city : "Seattle",
                            zip : "98101",
                            state : "WA"
                          }
                        ,
                        gallery : {
                          visibility : "Visible",
                          url : "egr.aphs/joshuaj"
                        },
                        notices : {new_stars : true,
                                   news : true ,
                                   mlb_updates : false
                                  },
                        social : {facebook : true,
                                  twitter : false,
                                  tumblr : true}
                        };

      $scope.update = function(user) {
        $scope.master = angular.copy(user);
      };

      $scope.reset = function() {
        $scope.user = angular.copy($scope.master);
      }

      $scope.resetVal = function(val) {
        var splitpath = val.split(".");
        var master = $scope.master;
        var userObj = $scope.user;
        //TODO use parse instead
        //Get second to last element to change the reference to the original master string.
        for(i = 1; i < splitpath.length - 1; i++){
          console.log(splitpath[i]);
          master = master[splitpath[i]];
          userObj = userObj[splitpath[i]];
        }
        console.log("splitpath " + splitpath);

        userObj[splitpath[splitpath.length - 1]] = angular.copy(master[splitpath[splitpath.length - 1]]);


      }

      $scope.reset();

      $scope.cancel = function (modelString, parentID) {
        var parent = $("#" + parentID);
        parent.addClass('hide');
        parent.prev().removeClass('hide');
        $scope.resetVal(modelString);
      }
    }
  }
});