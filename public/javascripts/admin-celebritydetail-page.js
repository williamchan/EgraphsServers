/* Scripting for the celebrity detail page */
define([], function() {

  return {
    /**
     * Executes all the scripts for the admin-celebritydetail page.

     * @return nada
     */
    go: function () {
      var publishButton = $("#publish-button");
      var publishStatus = $("#publishedStatus");

      var setPublishStatus = function(status){
        publishStatus.val(status);
        publishButton.text(status);
      };

      publishButton.text(publishStatus.val());

      if(publishButton === "Published"){
        publishButton.button("toggle");
      }

      publishButton.click(function() {
        switch (publishStatus.val()) {
            case "Published" :
              setPublishStatus("Unpublished");
              break;

            case "Unpublished" :
              setPublishStatus("Published");
              break;

            default:
              publishButton.text("Error");
              break;
        }

        publishButton.button("toggle");
        return false;
      })
    }
  };
});