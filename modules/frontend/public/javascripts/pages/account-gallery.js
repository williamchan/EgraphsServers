/* Scripting for the account_gallery page */
define(["services/forms", "Egraphs", "libs/twitter-bootstrap/bootstrap-button"],
function(forms, Egraphs) {

  var page = Egraphs.page;

  return {
    go: function() {
      var toggle_map = {
        true : 'Public',
        false  : 'Private'
      }
      var calloutSpeed =  500;

      var postCallback = function(data, id, callout, toggle) {
        console.log(data);
        if(data.privacyStatus === "Public" || data.privacyStatus === "Private"){
          callout.text("This egraph is now " + data.privacyStatus.toLowerCase() + ".");
        } else {
          callout.text("There was an error, please try again later");
        }
        callout.fadeIn(calloutSpeed);
        toggle.slideToggle();
      }

      forms.setIphoneCheckbox('.private', {
        checkedLabel: 'YES',
        uncheckedLabel: 'NO',
        onChange: function(elem, value) {
          var field = '.field';
          var id = $(elem).parents(field).attr("id");
          var callout  = $("#info-"+id)
          console.log("value: " + value);
          var toggle = $(elem).parents(field).siblings(field)
          //Send the request, on callback put check icon to indicate saved.
          $.ajax({ url:  "/orders/" + id + "/configure",
                   data: {privacyStatus : toggle_map[value]},
                   type: 'post',
                   error: function() {

                     callout.text("Connection error, try again later.");
                     callout.fadeIn(calloutSpeed);
                   },
                   success: function(data) { postCallback(data, id, callout, toggle); }
                 });
          }
        }
      );
    }
  }
})