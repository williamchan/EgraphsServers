define(["Egraphs"],
  function (Egraphs) {
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
      }
    }
  }
);