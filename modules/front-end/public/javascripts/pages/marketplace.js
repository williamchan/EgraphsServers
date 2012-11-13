define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    go: function () {
     $(document).ready(function() {

        // Function for reconstructing Url

        var reloadPage = function() {
          window.location.href = window.Egraphs.page.queryUrl + "?" + 
            $.param({ "query" : window.Egraphs.page.query, "sort" : window.Egraphs.page.sort}) + "&" +
            $.param(window.Egraphs.page.categories);
        };      

        // Enable chosen.js style selectors
        $(".chsn-select").chosen({no_results_text: "No results matched"});

        $(".verticals tbody tr").hover(function() {
          $(this).addClass('hover');
        }, function() {
          $(this).removeClass('hover'); 
        });

        // Mobile Sorting
        $("#sort-select").change(function(e) {
          window.Egraphs.page.sort = $(this).val();
          reloadPage();
        });

        // Link based sorting
        $(".sort-link").click(function(e){
          var selectedValue = $(this).attr("data-value");
          if(selectedValue !== window.Egraphs.page.sort) {
            window.Egraphs.page.sort = selectedValue;
          } else {
            window.Egraphs.page.sort = "";
          }
          reloadPage();
        });
        /**
         * Binds all links with class cv-link to refresh the page with the specified category value
         * as a further refinement to the query. 
        **/  
        $(".cv-link").click(function(e){
          var link = $(this);
          var category = window.Egraphs.page.categories["c" + link.attr("data-category")];          
          var catVal = parseInt(link.attr("data-categoryvalue"));  

          // Remove CategoryValue from Array if already present, otherwise push it on. 
          if($.inArray(catVal, category) > -1) {
            var idx = $.inArray(catVal, category);
            category.splice(idx, 1);
          } else {
            console.log("push");
            category.push(catVal);
          }

          reloadPage();
          e.preventDefault();  
        });  


      });
    }
  }
});