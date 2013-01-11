define(["Egraphs"], function (Egraphs) {
  /**
   * Base marketplace code shared by the marketplace landing page and the
  **/

  var marketplace = {};

  marketplace.reloadPage = function() {
    window.location.href =
      Egraphs.page.queryUrl + Egraphs.page.verticalSlug + "?" +
      $.param({
        "query" : Egraphs.page.query || "",
        "sort" : Egraphs.page.sort || "",
        "view" : Egraphs.page.view || "",
        "availableOnly" : Egraphs.page.availableOnly || false
    }) + "&" + $.param(Egraphs.page.categories);
  };

  marketplace.clearQuery = function() {
    Egraphs.page.query = "";
    mixpanel.track("Cleared query");
  };

  marketplace.selectSort = function(sort) {
    Egraphs.page.sort = sort;
    mixpanel.track("Sorted by", {name: sort});
  };

  marketplace.selectView = function(view) {
    Egraphs.page.view = view;
    mixpanel.track("Selected view", {name:view});
  };

  marketplace.toggleAvailableOnly = function(availableOnly) {
    Egraphs.page.availableOnly = availableOnly;
    mixpanel.track("Filter by available stars", {value: availableOnly});
  };

  marketplace.selectVertical = function(verticalSlug, id) {
    if(Egraphs.page.verticalSlug === "/" + verticalSlug) {
      Egraphs.page.verticalSlug = "";
      Egraphs.page.categories = {};
      mixpanel.track("Deselect Vertical", {name: id});
    } else {
      Egraphs.page.verticalSlug  = "/" + verticalSlug;
      Egraphs.page.categories = {};
      mixpanel.track("Select Vertical", {name: id});
    }
  };

  marketplace.updateCategories = function(catVal, category, vertical, catValString) {
    if($.inArray(catVal, category) > -1) {
      var idx = $.inArray(catVal, category);
      category.splice(idx, 1);
      mixpanel.track("Remove Category", {name: catValString});
    } else {
      category.push(catVal);
      Egraphs.page.verticalSlug = "/" + vertical;
      mixpanel.track("Add Category", {name: catValString});
    }
  };

  marketplace.clearCategoryByKey = function(categoryKey) {
    var category = Egraphs.page.categories[categoryKey];
    category.length = 0;
    mixpanel.track("Clear all categories");
  };

  marketplace.clearCategories = function() {
    var categories = Egraphs.page.categories;
    $.each(categories, function(catId, selectedValues) {
      selectedValues.length = 0;
    });
    mixpanel.track("Clear all categories");
  };

  return marketplace;
});

