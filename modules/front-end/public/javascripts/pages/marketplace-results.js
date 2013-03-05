/*global angular console mixpanel*/
define(["Egraphs", "pages/marketplace", "ngApp", "services/logging", "module", "libs/angular", "libs/waypoints.min", "libs/jquery-ui"],
function (Egraphs, marketplace, ngApp, logging, requireModule) {
  var log = logging.namespace(requireModule.id);

  /**
   * Filter for producing a price range string from a celebrity object.
   * $45-90 if two different prices (non-zero)
   * $90 if minimum price is zero
   * <empty-string> if the prices are both zero
   */
  ngApp.filter('priceRange', function() {
    return function(celebrity) {
      if(celebrity.minPrice === celebrity.maxPrice && celebrity.minPrice > 0) {
        return "$" + celebrity.minPrice;
      } else if(celebrity.minPrice != celebrity.maxPrice) {
        if(celebrity.minPrice > 0 ) {
          return "$" + celebrity.minPrice + "-" + celebrity.maxPrice;
        } else {
          return "$" + celebrity.maxPrice;
        }
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

  ngApp.directive('whenScrolled', function() {
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
    ngControllers: {
     /**
      * Define controller for marketplace
      * @param $scope Global Angular Scope
      */
      MarketplaceCtrl: ["$scope", function ($scope) {
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
            log("Loading celebrities");
            mixpanel.track("Loaded more results");
            if(count >= $scope.total) {
              mixpanel.track("Loaded all results");
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
      }]
    },

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
        
        $("#remove-query").click( function(e) {
          marketplace.clearQuery();
          marketplace.reloadPage();
        });

        /**
         * Binding for selecting different sort orders for results from the mobile selector
         **/
        $("#sort-select").change(function(e) {
          marketplace.selectSort($(this).val());
          marketplace.reloadPage();
        });

        /**
         * Same as above but for large, link-based view
         **/
        $(".sort-link").click(function(e) {
          var selectedValue = $(this).attr("data-value");
          if(selectedValue !== window.Egraphs.page.sort) {
            marketplace.selectSort(selectedValue);
          } else {
            marketplace.selectSort("");
          }
          marketplace.reloadPage();
        });

        /**
         * Switch to list view
         **/
        $("#list-view-link").click(function(e) {
          marketplace.selectView("list");
          marketplace.reloadPage();
        });

        /**
         * Switch to grid view
         **/
        $("#grid-view-link").click(function(e) {
          marketplace.selectView("");
          marketplace.reloadPage();
        });

        /**
         * Filter sold out stars
         **/
         $(".available-only a").click(function(e) {
           marketplace.toggleAvailableOnly($(this).attr("data-value"));
           marketplace.reloadPage();
         });

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
            var name = link.html().trim();
            var category = window.Egraphs.page.categories["c" + link.attr("data-category")];
            var catVal = parseInt(link.attr("data-categoryvalue"), 10);
            // Special use scenario...user searches, gets zero results, clicking on a category resets their search.
            if(Egraphs.page.results.celebrities.length === 0 && Egraphs.page.categorySelected === false){
              window.Egraphs.page.query = "";
            }
            marketplace.updateCategories(catVal, category, $(this).attr("data-vertical"), name);
            marketplace.reloadPage();
          }
        );

        $(".show-all").click(function(e){
          $(this).parent().siblings().children().each(function() {
            $(this).removeClass("condensed");
          });
          $(this).addClass("condensed");
        });

        $(".clear-all").click(function(e) {
          marketplace.clearCategoryByKey("c" + $(this).attr("data-category"));
          marketplace.reloadPage();
        });

      });
    }
  };
});