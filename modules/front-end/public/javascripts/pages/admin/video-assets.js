/* Scripting for the video assets page */
define(["Egraphs"], function() {

  return {
    /**
     * Executes all the scripts for the admin-video assets page.

     * @return nothing
     */
    go: function () {
      
      $("#see-unprocessed-button").click(function(e) {
        alert("ouch, you clicked me");
      });
    }
  };
});