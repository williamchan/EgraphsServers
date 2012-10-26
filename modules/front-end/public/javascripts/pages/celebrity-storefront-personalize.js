/* Scripting for the personalization page */
define(["services/forms", "Egraphs", "bootstrap/bootstrap-button"],
function(forms, Egraphs) {
  
  var page = Egraphs.page,
      celeb = page.celeb;

  /** Animation dynamics for the gift swap effect. */
  function setIsGift(isGift) {
    var visibleWhenIsGift = $('.gift-only');
    var visibleWhenIsNotGift = $('.non-gift-only');
    
    var toShow;
    var toHide;

    if (isGift) {
      toShow = visibleWhenIsGift;
      toHide = visibleWhenIsNotGift;
    } else {
      toShow = visibleWhenIsNotGift;
      toHide = visibleWhenIsGift;
    }
    
    forms.enableChildInputs(toShow, true);

    toShow.slideDown(250, function() {
      toHide.slideUp(250, function() {
        forms.enableChildInputs(toHide, false);
      });
    });
  }

  return {
    go: function() {
      forms.setIphoneCheckbox('#gift', {
        checkedLabel: 'YES',
        uncheckedLabel: 'NO',
        onChange: function(elem, isGift) {
          setIsGift(isGift);
        }
      });

      forms.setDefaultText(
        "#egraph-message textarea",
        celeb.name + " will write this on your egraph."
      );
      forms.setDefaultText(
        "#your-message",
        "Is there something you would like "+ celeb.name +" to know? The more info you give, the more personal the egraph will be."
      );
      forms.setAlert('.alert');
      forms.setButton('.btn-group', 'toggle');

      // change hidden input
      $('#message-options button').click(function() { $('#message-type').val($(this).val()); });
      
      // show message
      $('#message-options button:eq(0)').click(function() { $('#egraph-message').slideDown(); });
      
      // hide message
      $('#message-options button:not(:eq(0))').click(function() { $('#egraph-message').slideUp(); });

      forms.bindCounter("#egraph-message-text", "#egraph-message-count", Egraphs.page.egraphMessageLimit);
      forms.bindCounter("#your-message", "#your-message-count", Egraphs.page.yourMessageLimit);

      // Mixpanel events
      mixpanel.track_forms('.container form', 'Review clicked');
    }
  };
});
