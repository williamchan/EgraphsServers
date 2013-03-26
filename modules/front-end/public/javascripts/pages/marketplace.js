define(["page", "services/analytics"], function (page, analytics) {
  /**
   * Base marketplace code shared by the marketplace landing page and the results page.
   * The aim of most of the functions below is to retain the state of a search in the url bar.
   * If a user clicks list view, we want to reload the same results for whatever they were looking at before
   * in a list view. Likewise, if a user selects a new filter, we want all the previous selections to remain enabled.
   * The same idea applies to removing a filter or any other changes, with the exception of the search box, since the previous
   * settings may not be applicable and give a confusing set of results.
   *
  **/
  var events = analytics.eventCategory("Marketplace");
  var marketplace = {};

  marketplace.reloadPage = function() {
    window.location.href =
      page.queryUrl + page.verticalSlug + "?" +
      $.param({
        "query" : page.query || "",
        "sort" : page.sort || "",
        "view" : page.view || "",
        "availableOnly" : page.availableOnly || false
    }) + "&" + $.param(page.categories);
  };

  marketplace.clearQuery = function() {
    page.query = "";
    events.track(["Cleared query"]);
  };

  marketplace.selectSort = function(sort) {
    page.sort = sort;
    events.track(["Sorted by", sort]);
  };

  marketplace.selectView = function(view) {
    page.view = view;
    events.track(["Selected view", view]);
  };

  marketplace.toggleAvailableOnly = function(availableOnly) {
    page.availableOnly = availableOnly;
    events.track(["Filter by available stars", availableOnly]);
  };

  marketplace.selectVertical = function(verticalSlug, id) {
    if(page.verticalSlug === "/" + verticalSlug) {
      page.verticalSlug = "";
      page.categories = {};
      events.track(["Deselect Vertical", verticalSlug]);
    } else {
      page.verticalSlug  = "/" + verticalSlug;
      page.categories = {};
      events.track(["Select Vertical", verticalSlug]);
    }
  };

  marketplace.updateCategories = function(catVal, category, vertical, catValString) {
    if($.inArray(catVal, category) > -1) {
      var idx = $.inArray(catVal, category);
      category.splice(idx, 1);
      events.track(["Remove Category", catValString]);
    } else {
      category.push(catVal);
      page.verticalSlug = "/" + vertical;
      events.track(["Add Category", catValString]);
    }
  };

  marketplace.clearCategoryByKey = function(categoryKey) {
    var category = page.categories[categoryKey];
    category.length = 0;
    events.track(["Clear category"]);
  };

  marketplace.clearCategories = function() {
    var categories = page.categories;
    $.each(categories, function(catId, selectedValues) {
      selectedValues.length = 0;
    });
    events.track(["Clear category", "All"]);
  };

  return marketplace;
});

