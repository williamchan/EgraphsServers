define(["Egraphs", "libs/angular", "libs/chosen/chosen.jquery.min", "libs/waypoints.min"], function (Egraphs) {
  /**
   * Define controller for marketplace
   * @param $scope Global Angular Scope
   */
  var marketplaceCtrl = function ($scope) {
    $scope.results = angular.copy(Egraphs.page.results);
    $scope.total = $scope.results.celebrities.length;
    $scope.celebrities = [];
    var count = 0;
    var countIncrement = 0;

    /**
     * Depending on the screensize, set increment for infinite scroll
     * of celebrity result sets.
     * Desktop displays rows of 3, iPad-like rows of 2, and phone is 1.
     * Buffer two rows to keep scrolling smooth.
     **/
    if($(window).width() < 720) {
      countIncrement = 4;
    } else {
      countIncrement = 6;
    }

    /**
     *  Function for loading celebrities.
     *  Hides the see-more button when out of celebrities.
     *  Button serves as a manual override for when scroll events behave strangely.
     **/
    var loadCelebrities = function(incr) {
      if(count < $scope.total) {
        $scope.celebrities = $scope.celebrities.concat($scope.results.celebrities.slice(count, count + incr));
        count += incr;
        atBottom = false;
        if(count >= $scope.total) {
          $(".see-more").addClass("hidden");
        }
      }
    };

    /**
     * Creates a controller function with a fixed increment
    **/
    $scope.loadCelebrities = function() {
      loadCelebrities(countIncrement);
    };

    // Page in two sets of results.
    loadCelebrities(countIncrement*2);

  };

  /**
   * Define a angular module for the marketplace
   */
  var marketplaceModule = angular.module('marketplace', []);

  /**
   * Filter for producing a price range string from a celebrity object.
   * $45-90 if two different prices (non-zero)
   * $90 if minimum price is zero
   * <empty-string> if the prices are both zero
   */
  marketplaceModule.filter('priceRange', function() {
    return function(celebrity) {
      if(celebrity.minPrice === celebrity.maxPrice && celebrity.minPrice > 0) {
        return "$" + celebrity.minPrice;
      } else if(celebrity.minPrice != celebrity.maxPrice) {
        return "$" + celebrity.minPrice + "-" + celebrity.maxPrice;
      } else {
        return "";
      }
    };
  });

  /**
   * Directive for binding an element to scroll event.
   * Inspired by:
   * http://specificidea.com/collection_items/blog/infinite-scroll-with-angularjs-and-rails/59
   * Depends on waypoint library:
   * http://imakewebthings.com/jquery-waypoints/
   */
  var atBottom = false;

  marketplaceModule.directive('whenScrolled', function() {
    return function(scope, element, attrs) {
      $(".celebrity-result").last().waypoint(function(){
        if(atBottom === false && !scope.$$phase){
          atBottom = true;
          scope.$apply(attrs.whenScrolled);
          $(this).waypoint();
        }
      }, {offset: 'bottom-in-view', continuous: false, triggerOnce : false});
    };
  });

  return {
    go: function () {
      /**
       * The aim of most of the functions below is to retain the state of a search in the url bar.
       * If a user clicks list view, we want to reload the same results for whatever they were looking at before
       * in a list view. Likewise, if a user selects a new filter, we want all the previous selections to remain enabled.
       * The same idea applies to removing a filter or any other changes, with the exception of the search box, since the previous
       * settings may not be applicable and give a confusing set of results.
       **/

      // Angular Setup
      window.MarketplaceCtrl = marketplaceCtrl;
      angular.element(document).ready(function() {
        angular.bootstrap(document, ['marketplace']);
      });


     $(document).ready(function() {
        /**
         * Reconstructs url from any changes to data model.
         * Used to retain state across reloads.
         **/
        var reloadPage = function() {
          window.location.href =
            window.Egraphs.page.queryUrl + window.Egraphs.page.verticalSlug + "?" +
            $.param({ "query" : window.Egraphs.page.query,
                      "sort" : window.Egraphs.page.sort,
                      "view" : window.Egraphs.page.view,
                      "availableOnly" : window.Egraphs.page.availableOnly}) +
            "&" + $.param(window.Egraphs.page.categories);
        };
        
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
         * Filter sold out stars
         **/
         $(".available-only a").click(function(e) {
           window.Egraphs.page.availableOnly = $(this).attr("data-value");
           reloadPage();
         });

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
            window.Egraphs.page.verticalSlug  = selectedVerticalSlug;
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
            // Special use scenario...user searches, gets zero results, clicking on a category resets their search.
            if(window.Egraphs.page.results.celebrities.length === 0 && window.Egraphs.page.categorySelected === false){
              window.Egraphs.page.query = "";
            }
            updateCategories(catVal, category, $(this).attr("data-vertical"));
            reloadPage();
          }
        );

        $(".clear-all").click(function(e) {
          var category = window.Egraphs.page.categories["c" + $(this).attr("data-category")];
          category.length = 0;
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