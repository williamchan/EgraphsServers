/* Scripting for the account_gallery page */
define(["services/forms", "Egraphs", "bootstrap/bootstrap-button"],
  function (forms, Egraphs) {

    var page = Egraphs.page;
    var calloutSpeedms =  1000;
    //Map checkbox values to privacy enums
    var toggle_map = {
      'true'   : 'Public',
      'false'  : 'Private'
    };

    //Report error flag from server or acknowledgement of new value.
    var postCallback = function (data, id, callout, toggle) {
      if (data.privacyStatus === "Public" || data.privacyStatus === "Private") {
        callout.text("This egraph is now " + data.privacyStatus.toLowerCase() + ".");
      } else {
        callout.text("There was an error, please try again later");
      }
      callout.fadeIn(calloutSpeedms);
      toggle.slideToggle();
    };

    return {
      go: function () {

        forms.setIphoneCheckbox('.private', {
          checkedLabel: 'YES',
          uncheckedLabel: 'NO',
          onChange: function (elem, value) {
            var field = '.field';
            var id = $(elem).parents(field).attr("id");
            var callout  = $("#info-" + id);
            var toggle = $(elem).parents(field).siblings(field);

            //Send the request, on callback indicate success
            $.ajax({
              url:  "/orders/" + id + "/configure",
              data: {privacyStatus : toggle_map[value], authenticityToken : Egraphs.page.authenticityToken},
              type: 'post',
              //404s and other connection issues
              error: function () {
                callout.text("Connection error, try again later.");
                callout.fadeIn(calloutSpeedms);
              },
              success: function (data) {
                postCallback(data, id, callout, toggle);
              }
            });
          }
        });
      }
    };
  });