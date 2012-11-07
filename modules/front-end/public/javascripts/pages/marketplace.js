define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    go: function () {
     $(document).ready(function() {

        // Enable chosen.js style selectors
        $(".chsn-select").chosen({no_results_text: "No results matched"});

        $(".verticals tbody tr").hover(function() {
          $(this).addClass('hover');
        }, function() {
          $(this).removeClass('hover'); 
        });
        /**
         * Binds all links with class cv-link to refresh the page with the specified category value
         * as a further refinement to the query. 
        **/  
        $(".cv-link").click(function(e){
          var link = $(this);
          var catId = link.attr("data-category");
          var catVal = link.attr("data-categoryvalue");  
          var categories = window.Egraphs.page.categories
          
          categories["c" + catId].push(catVal);
          window.location.href = window.Egraphs.page.queryUrl + "?" + $.param(categories);
          e.preventDefault();  
        });  

        /**
         *  TODO: Further refinement functions such as a Vertical. 
         **/ 
      });
    }
  }
});