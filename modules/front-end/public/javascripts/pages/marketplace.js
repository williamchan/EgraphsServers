define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    applyFilter: function(filterId, filterValue) {
      Egraphs.page.categories["c" + filterId].push(filterValue);
      window.location.href = window.location.href+"?"+$.param(Egraphs.page.filters);   
    },

    go: function () {
     $(document).ready(function() {

        // Enable chosen.js style selectors
        $(".chsn-select").chosen({no_results_text: "No results matched"});

        $(".verticals tbody tr").hover(function() {
          $(this).addClass('hover');
        }, function() {
          $(this).removeClass('hover'); 
        });

      });
    }
  }
});