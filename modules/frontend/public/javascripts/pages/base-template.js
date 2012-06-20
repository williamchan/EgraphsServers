/* Scripting for the base template page */
define([], function() {

  return {
    /**
     * Executes all the scripts for the base template.
     *
     * @return nothing
     */
    go: function () {
      $(document).ready(function(){
        // highlight action on top menu
        $('#top .account').hover(function(){
          $(this).addClass('hover');
        }, function(){
          $(this).removeClass('hover');
        });

        // show top account menu on click
        $('#top .account').click(function(e){
          var account_options = $(this).find('.account-options');
          $('body').one('click',function(){
            account_options.removeClass('active');
          });
          account_options.addClass('active');
          e.stopPropagation();
          e.preventDefault();
        });

        // smooth scroll to top
        $('.to-top a').click(function(e){
            $('html,body').animate({scrollTop: $($(this).attr('href')).offset().top},'slow');
            e.preventDefault();
        });
      });
    }
  };
});