/* Scripting for the celebrity detail page */
define(
  ["Egraphs",
   "bootstrap/bootstrap-alert",
   "bootstrap/bootstrap-button",
   "bootstrap/bootstrap-collapse",
   "libs/chosen/chosen.jquery.min",
   "libs/jeditable/jquery.jeditable.masked",
   "services/forms"],
function(Egraphs) {
  var initPublishButton = function() {
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
    });

    $('.chzn-select').chosen();
  };

  var initEditableDelay = function() {
    $(".editableDelayInDays").editable(Egraphs.page.postExpectedOrderDelayUrl, {
      name: 'delayInDays',
      type: 'masked',
      mask: '9?99',
      submitdata: Egraphs.page.authToken,
      submit: 'Save',
      cancel: 'Discard',
      onblur: 'ignore'
    });
  };

  return {

    /**
     * Executes all the scripts for the admin-celebritydetail page.

     * @return nothing
     */
    go: function () {
      $(document).ready(function() {
        initPublishButton();
        initEditableDelay();
      });
    }
  };
});