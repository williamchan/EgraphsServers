define(["Egraphs"],
  function (Egraphs) {
    var toc = $("#toc");
         var links = $(".toc-anchor");
         var link_pos = {};
         for(var i = 0; i < links.size(); i++){
           link_pos[$(links[i]).offset().top] = $(links[i]);
         }

    var selectLink = function (link) {
      $(".toc-link").removeClass("selected");
      $(".arrow-right").addClass("invisible");
      link.addClass("selected");
      link.prev().removeClass("invisible");
    }

    return {
       go: function() {
          //highlight and make the arrow visible on the section that was selected.
          $(".toc-link").click(
          function (e) {
            var link = $(this);
            selectLink(link);
          }
        );
        //Keep the menu in a truly fantastic spot (towards the top)
        $(window).scroll(function() {

          var top = $(window).scrollTop();
          //the menu is only sticky at wide widths
          if(top < 450){
            toc.css('top', 450-top);
          } else {
            toc.css('top', 0);
          }
          //highlight the correct link when you reach the spot on the page.link_+pos
          var min = Number.MAX_VALUE;
          var mLink;
          for(i in link_pos) {
            if(i - top < min) {
              min = top - i;
              mLink = link_pos[i];

            }
          }
          selectLink($('a[href="#' + mLink.attr('id') + '"]'));

        });

        $(window).scroll();



      }
    }
  }
);