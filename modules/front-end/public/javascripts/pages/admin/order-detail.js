/* Scripting for the celebrity detail page */
define(["Egraphs", "bootstrap/bootstrap-alert", "bootstrap/bootstrap-button", "services/forms"], function() {

  return {
    /**
     * Executes all the scripts for the admin-order page.

     * @return nothing
     */
    go: function () {
      var privacyButton = $("#privacy-button");
      var privacyStatus = $("#privacyStatus");

      var setPrivacyStatus = function(status){
        privacyStatus.val(status);
        privacyButton.text(status);
      };

      privacyButton.text(privacyStatus.val());

      if(privacyButton === "Public"){
        privacyButton.button("toggle");
      }

      privacyButton.click(function() {
        switch (privacyStatus.val()) {
          case "Public" :
            setPrivacyStatus("Private");
            break;

          case "Private" :
            setPrivacyStatus("Public");
            break;

          default:
            privacyButton.text("Error");
            break;
        }
        privacyButton.button("toggle");
        return false;
      });
    }
  };
});