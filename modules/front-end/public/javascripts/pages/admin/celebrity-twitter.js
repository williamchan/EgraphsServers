/* Scripting for the celebrity detail page */
define(
  ["Egraphs",
   "libs/chosen/chosen.jquery.min",
   "libs/jeditable/jquery.jeditable"],
function(Egraphs) {
  var initEditableOfficialTwitterScreenName = function() {
    $(".editableOfficialTwitterScreenName").each( function() {
      var thisElem = $(this);
      var url = thisElem.attr("data-url");
      thisElem.editable(url, {
        name: 'officialTwitterScreenName',
        type: 'textarea',
        submitdata: Egraphs.page.authToken,
        submit: 'Save',
        cancel: 'Discard',
        onblur: 'ignore'
      });
    });
  };

  return {

    /**
     * Executes all the scripts for the admin-celebrity-twitter page.

     * @return nothing
     */
    go: function () {
      $(document).ready(function() {
        initEditableOfficialTwitterScreenName();
      });
    }
  };
});