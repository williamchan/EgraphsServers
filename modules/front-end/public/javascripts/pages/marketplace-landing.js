define(["Egraphs", "libs/chosen/chosen.jquery.min"], function (Egraphs) {
 
  return {
    go: function () {
      /**
       * This page represents a subset of the functionality of marketplace.js
       * Clicks on the marketplace landing page only select one category and do not retain state.
       **/

     $(document).ready(function() {
        /**
         * Reconstructs url from any changes to data model.
         * Used to retain state across reloads.
         **/
        var reloadPage = function() {
          window.location.href =
            window.Egraphs.page.queryUrl + window.Egraphs.page.verticalSlug + "?" +
            $.param({ "query" : window.Egraphs.page.query}) +
            "&" + $.param(window.Egraphs.page.categories);
        };
        
       /**
        * Vertical button select functionality.
        *
        */
       $(".vertical-button").click(function(e) {
          var selectedVerticalSlug = "/" + $(this).attr("data-vertical");
          if(window.Egraphs.page.verticalSlug === selectedVerticalSlug){
            window.Egraphs.page.verticalSlug = "";
            window.Egraphs.page.categories = {};
          } else {
            window.Egraphs.page.verticalSlug = selectedVerticalSlug;
            window.Egraphs.page.categories = {};
          }
          reloadPage();
       });

        /**
         * Helper for updating the currently selected CategoryValues
         **/
        var updateCategories = function(catVal, category, vertical) {
          if($.inArray(catVal, category) > -1) {
            var idx = $.inArray(catVal, category);
            category.splice(idx, 1);
          } else {
            category.push(catVal);
            window.Egraphs.page.verticalSlug = "/" + vertical;
          }
        };
        
        /**
         * Binds apply filters link to processing the multiple select widget visible on resolutions < 720px.
         * Unlike the categories on the larger view, a user can select more than one at a time.
         **/
        $("#apply-filters").click(function(e) {
          // Clear out previous category values
          var categories = window.Egraphs.page.categories;
          $.each(categories, function(cat) {
            cat.length = 0;
          });
          // Apply new selections
          $("option:selected", "#category-select").each(
            function(index) {
              var category = categories["c" + $(this).attr("data-category")];
              var catVal = $(this).val();
              updateCategories(catVal, category, $(this).attr("data-vertical"));
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
            
            updateCategories(catVal, category, $(this).attr("data-vertical"));
            reloadPage();
          }
        );

        $(".show-all").click(function(e){
          $(this).parent().siblings().children().each(function() {
            $(this).removeClass("condensed");
          });
          $(this).addClass("condensed");
        });
      });
    }
  };
});