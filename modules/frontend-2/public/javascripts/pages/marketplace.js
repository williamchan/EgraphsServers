define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    go: function () {
     $(document).ready(function() {

        // Enable chosen.js style selectors
        $(".chsn-select").chosen({no_results_text: "No results matched"});

      });
    }
  }
});