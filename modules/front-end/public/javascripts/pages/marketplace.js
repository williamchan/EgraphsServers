define(["libs/chosen/chosen.jquery.min"], function (Egraphs) {
  return {
    go: function () {
      /**
       * The aim of most of the functions below is to retain the state of a search in the url bar.
       * If a user clicks list view, we want to reload the same results for whatever they were looking at before
       * in a list view. Likewise, if a user selects a new filter, we want all the previous selections to remain enabled.
       * The same idea applies to removing a filter or any other changes, with the exception of the search box, since the previous
       * settings may not be applicable and give a confusing set of results.
       **/
     $(document).ready(function() {
        /**
         * Reconstructs url from any changes to data model.
         * Used to retain state across reloads.
         **/
        var reloadPage = function() {
          window.location.href =
            window.Egraphs.page.queryUrl + "?" +
            $.param({ "query" : window.Egraphs.page.query, "sort" : window.Egraphs.page.sort, "view" : window.Egraphs.page.view}) + "&" +
            $.param(window.Egraphs.page.categories);
        };
        
        /**
         * Hover effect on list view
         **/
        $(".verticals tbody tr").hover(function() {
          $(this).addClass('hover');
        }, function() {
          $(this).removeClass('hover');
        });

        $("#remove-query").click( function(e) {
          window.Egraphs.page.query = "";
          reloadPage();
        });

        /**
         * Binding for selecting different sort orders for results from the mobile selector
         **/
        $("#sort-select").change(function(e) {
          window.Egraphs.page.sort = $(this).val();
          reloadPage();
        });

        /**
         * Same as above but for large, link-based view
         **/
        $(".sort-link").click(function(e) {
          var selectedValue = $(this).attr("data-value");
          if(selectedValue !== window.Egraphs.page.sort) {
            window.Egraphs.page.sort = selectedValue;
          } else {
            window.Egraphs.page.sort = "";
          }
          reloadPage();
        });

        /**
         * Switch to list view
         **/
        $("#list-view-link").click(function(e) {
          window.Egraphs.page.view = "list";
          reloadPage();
        });

        /**
         * Switch to grid view
         **/
        $("#grid-view-link").click(function(e) {
          window.Egraphs.page.view = "";
          reloadPage();
        });

        /**
         * Helper for updating the currently selected CategoryValues
         **/
        var updateCategories = function(catVal, category) {
          if($.inArray(catVal, category) > -1) {
            var idx = $.inArray(catVal, category);
            category.splice(idx, 1);
          } else {
            category.push(catVal);
          }
        };
        
        /**
         * Binds apply filters link to processing the multiple select widget visible on resolutions < 720px.
         * Unlike the categories on the larger view, a user can select more than one at a time.
         **/
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
     

