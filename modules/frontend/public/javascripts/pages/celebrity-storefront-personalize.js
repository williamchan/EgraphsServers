/* Scripting for the personalization page */
define(["services/forms", "Egraphs", "libs/twitter-bootstrap/bootstrap-button"],
function(forms, Egraphs) {
  
  var page = Egraphs.page,
      celeb = page.celeb;

  return {
    go: function() {
      forms.setIphoneCheckbox(':checkbox');
      forms.setDefaultText("#egraph-message textarea", "This is the message that "+celeb+" will write on your egraph.");
      forms.setDefaultText("#your-message", "Write here to tell "+celeb+" something you’d like him to know.");
      forms.setAlert('.alert');
    }    
  };
  
});