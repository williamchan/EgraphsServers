/* Scripting for the login page */
define(["services/forms", "Egraphs", "services/analytics", "libs/jquery-ui"],
function(forms, Egraphs, analytics) {
  // This is mostly scripting to enable a nice experience for new registrants to our
  // site.
  var pageDidLoadWithRegistrationErrors = Egraphs.page.login.registrationErrors;
  var pageIsExtollingRegistrationBenefits = pageDidLoadWithRegistrationErrors? false: true;
  var mailEvents = analytics.eventCategory("Mail");

  var $registerForm = function() { return $("#new-account-form"); };

  var setRegisterButtonText = function(text) {
    $('.button.create-account em').text(text);
  };

  var isSubscribingToNewsletter = function() {
    var checkbox = $('#bulk-email')[0];
    if (!checkbox) return false;
    else return checkbox.checked;
  };

  return {
    go: function() {
      $(document).ready(function() {
        if (!pageIsExtollingRegistrationBenefits) {
          setRegisterButtonText("Submit");
        }

        $registerForm().submit(function() {
          try {
            if (pageIsExtollingRegistrationBenefits) {
              // If we're currently extolling the benefits of registration then form
              // "submission" will actually just reveal the form.
              $('.new-account-benefits').hide("slide", {direction: "left"}, 200, function() {
                
                $('.new-account-fields').show("slide", {direction: "right"}, 200, function() {
                  pageIsExtollingRegistrationBenefits = false;
                  setRegisterButtonText("Register");

                  $("#register-email-field input").focus();
                  $('.registration-instructions').delay(250).show("slide", {direction: "up"}, 500);

                });
              });

              return false;
            } else { // !pageIsExtollingRegistrationBenefits
              // If the page didn't load extolling registration benefits, it means the user probably
              // had some error or other in a previously submitted form. Let's let this sucker submit.

              if (isSubscribingToNewsletter()) {
                mailEvents.track(['Subscribed to newsletter', 'Account registration']);
              }

              return true;
            }
          } catch(e) {
            return false;
          }
        });
        forms.setAlert('.alert');
      });
    }
  };
});