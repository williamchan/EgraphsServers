define(["Egraphs"],
  function (Egraphs) {
    var toc = $("#toc");
    return {
       go: function() {
          //highlight and make the arrow visible on the section that was selected.
          $(".toc-link").click(
          function (e) {
            var link = $(this);
            $(".toc-link").removeClass("selected");
            $(".arrow-right").addClass("invisible");
            link.addClass("selected");
            link.prev().removeClass("invisible");
          }
        );
        //Keep the menu in a truly fantastic spot (towards the top)
        $(window).scroll(function() {
          var top = $(window).scrollTop();
          if(top < 450){
            toc.css('top', 450-top);
          } else {
            toc.css('top', 0);
          }
        });


      }
    }
  }
);