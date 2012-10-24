/* Scripting for the celebrity detail page */
define(["Egraphs", "bootstrap/bootstrap-alert", "bootstrap/bootstrap-button", "libs/chosen/chosen.jquery.min", "services/forms"], function() {

  var confirmPost = function() {
    confirm("Updating this product will also change all associated egraphs! Are you sure you want to save these changes?");
  };

  return {
    /**
     * Executes all the scripts for the admin-celebritydetail page.

     * @return nothing
     */
    go: function () {
      var publishButton = $("#publish-button");
      var publishStatus = $("#publishedStatus");
      var productDetail = $("#product-detail");

      productDetail.submit(confirmPost);

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
      });
    }
  };
});