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
          var category = window.Egraphs.page.categories["c" + link.attr("data-category")];          
          console.log(category);
          var catVal = parseInt(link.attr("data-categoryvalue"));  
          console.log(catVal);  

          if($.inArray(catVal, category) > -1) {
            var idx = $.inArray(catVal, category);
            category.splice(idx, 1);
            console.log("splice");
          } else {
            console.log("push");
            category.push(catVal);
          }
          window.location.href = window.Egraphs.page.queryUrl + "?" + $.param(window.Egraphs.page.categories)+ 
            "&" + $.param({ "query" : window.Egraphs.page.query});
          e.preventDefault();  
        });  

        /**
         *  TODO: Further refinement functions such as a Vertical. 
         **/ 
      });
    }
  }
});