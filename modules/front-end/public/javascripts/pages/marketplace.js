define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    go: function () {
     $(document).ready(function() {

        // Function for reconstructing Url
        var reloadPage = function() {
          window.location.href =
            window.Egraphs.page.queryUrl + "?" +
            $.param({ "query" : window.Egraphs.page.query, "sort" : window.Egraphs.page.sort, "view" : window.Egraphs.page.view}) + "&" +
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
        $(".sort-link").click(function(e) {
          var selectedValue = $(this).attr("data-value");
          if(selectedValue !== window.Egraphs.page.sort) {
            window.Egraphs.page.sort = selectedValue;
          } else {
            window.Egraphs.page.sort = "";
          }
          reloadPage();
        });

        $("#list-view-link").click(function(e) {
          window.Egraphs.page.view = "list";
          reloadPage();
        });

        $("#grid-view-link").click(function(e) {
          window.Egraphs.page.view = "";
          reloadPage();
        });

        // Remove CategoryValue from Array if already present, otherwise push it on.
        var updateCategories = function(catVal, category) {
          if($.inArray(catVal, category) > -1) {
            var idx = $.inArray(catVal, category);
            category.splice(idx, 1);
          } else {
            category.push(catVal);
          }
        };

        $("#apply-filters").click(function(e) {
          // Clear out previous category values
          var categories = window.Egraphs.page.categories;
          for(var cat in categories) {
            categories[cat].length = 0;
          }
          // Apply new selections
          $("option:selected", "#category-select").each(
            function(index) {
              var category = categories["c" + $(this).attr("data-category")];
              var catVal = $(this).val();
              updateCategories(catVal, category);
            }
          );
          
          reloadPage();
        });

        /**
         * Binds all links with class cv-link to refresh the page with the specified category value
         * as a further refinement to the query.
        **/
        $(".cv-link").click(
          function(e) {
            var link = $(this);
            var category = window.Egraphs.page.categories["c" + link.attr("data-category")];
            var catVal = parseInt(link.attr("data-categoryvalue"), 10);
            updateCategories(catVal, category);
            reloadPage();
          }
        );
      });
    }
  };
});
     

