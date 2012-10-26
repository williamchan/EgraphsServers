/* Scripting for the complete page */
define(["Egraphs", "bootstrap/bootstrap-modal"],
function(forms, Egraphs) {

  return {
    go: function() {
      var jobPostings = $('.resumator-job-view-details >a');
      for (i = 0; i < jobPostings.length; i++) {
        jobPostings[i].click();
      }
    }
  };
});
