/* Scripting for the personalization page */
define(["services/forms", "Egraphs", "libs/twitter-bootstrap/bootstrap-button"],
function(forms, Egraphs) {
  
  var page = Egraphs.page,
      celeb = page.celeb;

  return {
    go: function() {
      forms.setIphoneCheckbox('#gift', { 
      	checkedLabel: 'YES', 
      	uncheckedLabel: 'NO',
      	onChange: function(elem, value) {
      		var field = '.field';
      		$(elem).parents(field).siblings(field).slideToggle();
        }
      });
      forms.setDefaultText("#egraph-message textarea", "This is the message that "+celeb+" will write on your egraph.");
      forms.setDefaultText("#your-message", "Write here to tell "+celeb+" something you’d like him to know.");
      forms.setAlert('.alert');
      forms.setButton('.btn-group', 'toggle');
    }    
  };
  
});

// change hidden input
$('#message-options button').click(function() { $('#message-type').val($(this).val()); });
// show message
$('#message-options button:eq(0)').click(function() { $('#egraph-message').show(); });
// hide message
$('#message-options button:not(:eq(0))').click(function() { $('#egraph-message').hide(); });