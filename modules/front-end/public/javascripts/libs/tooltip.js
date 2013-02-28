// Tooltip library adapted from http://osvaldas.info/elegant-css-and-jquery-tooltip-responsive-mobile-friendly
/*global angular */
define(
[
  "services/analytics",
  "window"
],
function(analytics, window) {
  var noop = angular.noop;

  return {
    apply: function(config) {
      var _config = angular.extend({
        selector: '[rel~=tooltip]',
        analyticsCategory: window.location.pathname
      }, config);
      var selector = _config.selector;

      var targets = $( selector ),
        target  = false,
        tooltip = false,
        remove_current = angular.noop,
        title   = false,
        tip     = false,
        openTooltip = function()
      {
        if(target) { remove_current(); }

        target = $( this );
        tip   = target.attr( 'title' );
        tooltip = $( '<div id="tooltip"></div>' );
        var analyticsEventName = target.attr("event")? target.attr("event"): target.html();
        var durationEvent = _config.analyticsCategory?
          analytics.eventCategory(_config.analyticsCategory).startEvent(["Tooltip", analyticsEventName]):
          {track: angular.noop};
     
        if( !tip || tip == '' )
          return false;
     
        target.removeAttr( 'title' );
        tooltip.css( 'opacity', 0 )
             .html( tip )
             .appendTo( 'body' );
     
        var init_tooltip = function()
        {
          if( $( window ).width() < tooltip.outerWidth() * 1.5 )
            tooltip.css( 'max-width', $( window ).width() / 2 );
          else
            tooltip.css( 'max-width', 340 );
     
          var pos_left = target.offset().left + ( target.outerWidth() / 2 ) - ( tooltip.outerWidth() / 2 ),
            pos_top  = target.offset().top - tooltip.outerHeight() - 20;
     
          if( pos_left < 0 )
          {
            pos_left = target.offset().left + target.outerWidth() / 2 - 20;
            tooltip.addClass( 'left' );
          }
          else
            tooltip.removeClass( 'left' );
     
          if( pos_left + tooltip.outerWidth() > $( window ).width() )
          {
            pos_left = target.offset().left - tooltip.outerWidth() + target.outerWidth() / 2 + 20;
            tooltip.addClass( 'right' );
          }
          else
            tooltip.removeClass( 'right' );
     
          if( pos_top < 0 )
          {
            var pos_top  = target.offset().top + target.outerHeight();
            tooltip.addClass( 'top' );
          }
          else
            tooltip.removeClass( 'top' );
     
          tooltip.css( { left: pos_left, top: pos_top } )
               .animate( { top: '+=10', opacity: 1 }, 50 );
        };
     
        init_tooltip();
        $( window ).resize( init_tooltip );
     
        var remove_tooltip = function()
        {
          tooltip.animate( { top: '-=10', opacity: 0 }, 50, function()
          {
            $( this ).remove();
          });
     
          target.attr( 'title', tip );
          
          //target.unbind('click', remove_tooltip);
          //tooltip.unbind('click', remove_tooltip);
          $('body').unbind('touchstart click', remove_tooltip);
          target.unbind('click', remove_tooltip);
          target.bind('click', openTooltip);

          durationEvent.track();
          target = false;
        };

        target.unbind('click', openTooltip);
        target.bind('click', remove_tooltip);
        $('body').bind( 'touchstart click', remove_tooltip);
        remove_current = remove_tooltip;
        // target.bind( 'click', remove_tooltip );
        // tooltip.bind( 'click', remove_tooltip );
      };
     
      targets.bind('click', openTooltip);
    }
  };
});