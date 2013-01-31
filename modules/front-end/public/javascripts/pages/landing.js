/* Scripting for the landing page */
define(["Egraphs",
 "pages/marketplace",
 "services/logging",
 "module",
 "bootstrap/bootstrap-tooltip",
 "bootstrap/bootstrap-popover",
 "services/responsive-modal"],
function (Egraphs, marketplace, logging, requireModule) {
  /**
   * Functions for the new landing page that share dependencies with the marketplace.
   * Marketplace.js contains mixpanel tracking events.
   **/
  var log = logging.namespace(requireModule.id);

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
      var category = window.Egraphs.page.categories["c" + link.attr("data-category")];
      var catVal = parseInt(link.attr("data-categoryvalue"), 10);
      marketplace.updateCategories(catVal, category, $(this).attr("data-vertical"));
      marketplace.reloadPage();
  };

  return {
    go: function() {
      $(document).ready(function() {
        
        $(".play-egraph").click(function() {
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
          mixpanel.track("Watched homepage video");
        };

        // Initialize the YouTube video and start playing.
        // See this YouTube Iframe API reference
        // https://developers.google.com/youtube/iframe_api_reference
        //

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

        // Mixpanel events
        mixpanel.track('Home page viewed', {'query': window.location.search });
        mixpanel.track_links('#get-started-button', 'Get Started clicked');
        $(".celebrities figure>a").click(function() {
          mixpanel.track("Celebrity clicked");
        });
        $(".celebrities h4>a").click(function() {
          mixpanel.track("Celebrity clicked");
        });
        
        $(".switcher-tab").click(function(e){
          var tab = $(this);
          var parent = tab.parent();
          parent.addClass("active");
          parent.siblings().removeClass("active");

          $(".tab").hide();
          $(tab.attr("href")).fadeIn();
          e.preventDefault();
        });

        $("a.helper").click(function() {
          $("#learn-switch").click();
        });
        $(".vertical-button").click(verticalFunction);
        $(".all-teams").click(verticalFunction);
        $(".vertical-tile").click(verticalFunction);
        $(".cv-link").click(categoryFunction);

      });
    }
  };
});