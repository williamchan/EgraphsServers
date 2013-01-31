/* Provides social media links in template footer */
define(["window"], function(window) {

  return {
    insertIntoPage: function() {
      // Facebook
      setTimeout(function() {
        (function(d, s, id) {
          var js, fjs = d.getElementsByTagName(s)[0];
          if (d.getElementById(id)) return;
          js = d.createElement(s); js.id = id;
          js.async = true;
          js.src = "//connect.facebook.net/en_US/all.js#xfbml=1&appId=156115741184892";
          fjs.parentNode.insertBefore(js, fjs);
        }(window.document, 'script', 'facebook-jssdk'));

        // Twitter
        !function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0];if(!d.getElementById(id)){js=d.createElement(s);js.async=true;js.id=id;js.src="//platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(window.document,"script","twitter-wjs");
      }, 1000);
    }
  };
});