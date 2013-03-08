/* Scripting for the landing page */
define(["page",
 "pages/marketplace",
 "services/analytics",
 "services/logging",
 "module",
 "bootstrap/bootstrap-tooltip",
 "bootstrap/bootstrap-transition",
 "services/responsive-modal"],
function (page, marketplace, analytics, logging, requireModule) {
  /**
   * Functions for the new landing page that share dependencies with the marketplace.
   * Marketplace.js contains mixpanel tracking events.
   **/
  var log = logging.namespace(requireModule.id);
  var events = analytics.eventCategory("Landing");

  // Select a vertical
  var verticalFunction = function(e) {
    var vertical = $(this);
    var slug =  vertical.attr("data-vertical");
    var id = vertical.attr("id");
    marketplace.selectVertical(slug, name);
    marketplace.reloadPage();
  };

  // Select a category value.
  var categoryFunction = function(e) {
      var link = $(this);
      var category = page.categories["c" + link.attr("data-category")];
      var catVal = parseInt(link.attr("data-categoryvalue"), 10);
      marketplace.updateCategories(catVal, category, $(this).attr("data-vertical"));
      marketplace.reloadPage();
  };

  return {
    go: function() {
      $(document).ready(function() {
        // Set up to enable the video modal. Add a selector here if you need a different
        // link to activate the modal.
        $(".play-egraph a, #play-video-link").click(function() {
          $("html, body").animate({ scrollTop: 0 }, "slow");
          $("#video-modal").responsivemodal('toggle');
        });

        var apiLoaded = false;

        var createPlayer = function(playerReadyCallback) {
          return new YT.Player('egraph-video', {
            videoId: 'BfU_cE6HxDw',
            events: {'onReady': playerReadyCallback}
          });
        };

        var onPlayerReady = function(event) {
          $("#video-modal").on('hide', function() {
            // Remove the player iframe to avoid weirdness around iframes being hidden.
            $("#egraph-video").replaceWith('<div id="egraph-video"></div>');
          });

          event.target.seekTo(7.0);
          event.target.playVideo();
          events.track(['Watched Video']);
        };

        // Initialize the YouTube video and start playing.
        // See this YouTube Iframe API reference
        // https://developers.google.com/youtube/iframe_api_reference

        $("#video-modal").on('shown', function() {
          var player;
          // Load the API selectively and create the YT object in a callback.
          if(apiLoaded === false) {
            var tag = document.createElement('script');
            tag.src = "//www.youtube.com/iframe_api";
            var firstScriptTag = document.getElementsByTagName('script')[0];
            firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

            window.onYouTubeIframeAPIReady = function() {
              apiLoaded = true;
              log("API ready");
              player = createPlayer(onPlayerReady);
            };
          } else {
            player = createPlayer(onPlayerReady);
          }
        });

        $(".soldout-tooltip").tooltip({placement:"top"});

        // Tracking events
        mixpanel.track('Home page viewed', {'query': window.location.search });

        $(".celebrities figure>a, .celebrities h4>a").click(function() {
          events.track(["Featured star clicked", $(this).attr("data-name")]);
        });
        
        // Configure learn/shop tab
        var selectTab = function(tabId) {
          var newTab = $(tabId + "-tab");
          newTab.addClass("active");
          newTab.siblings().removeClass("active");
          $(".tab").hide();
          $(tabId).fadeIn();
          events.track(["Tab selected", tabId]);
        };

        $(".switcher-tab").click(function(e){
          var tabId = $(this).attr("href");
          selectTab(tabId);
          e.preventDefault();
        });

        $("#learn-more-link").click(function(e) {
          $("html, body").animate({ scrollTop: 0 });
          selectTab("#learn");
          e.preventDefault();
        });
        
        // Configure shopping links
        $(".vertical-button").click(verticalFunction);
        $(".all-teams").click(verticalFunction);
        $(".vertical-tile").click(verticalFunction);
        $(".cv-link").click(categoryFunction);

         events.track(['Masthead viewed', page.mastheadName || "blank"]);

      });
    }
  };
});