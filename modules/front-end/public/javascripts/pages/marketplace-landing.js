define(["Egraphs", "pages/marketplace"], function (Egraphs, marketplace) {

  return {
    go: function () {
      /**
       * This page represents a subset of the functionality of marketplace.js
       * Clicks on the marketplace landing page only select one category and do not retain state.
       **/

     $(document).ready(function() {
      /**
        * Vertical button select functionality.
        *
        */
       $(".vertical-button").click(function(e) {
          var vertical = $(this);
          var slug =  vertical.attr("data-vertical");
          var id = vertical.attr("id");
          marketplace.selectVertical(slug, id);
          marketplace.reloadPage();
       });

        /**
         * Binds apply filters link to processing the multiple select widget visible on resolutions < 720px.
         * Unlike the categories on the larger view, a user can select more than one at a time.
         **/
        $("#apply-filters").click(function(e) {
          // Clear out previous category values
          marketplace.clearCategories();

          // Apply new selections
          $("#category-select").find(":selected").each(
            function(index) {
              var categoryId = $(this).attr("data-category");
              var categoryValues = Egraphs.page.categories["c" + categoryId];
              var catVal = parseInt($(this).val(), 10);
              marketplace.updateCategories(catVal, categoryValues);
            }
          );
          marketplace.reloadPage();
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

            marketplace.updateCategories(catVal, category, $(this).attr("data-vertical"));
            marketplace.reloadPage();
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