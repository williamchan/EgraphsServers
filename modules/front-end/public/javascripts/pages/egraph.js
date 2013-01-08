/* Scripting for the egraph page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {

      var playSound = function() {
        $(".sm2-360btn").click();
        guiders.hideAll();
      };

      /**
       * Create a guider to highlight the Play button on the Egraph page.
       **/
      if(Egraphs.page.isPromotional === true) {
        guiders.createGuider({
          attachTo: "#360player",
          classString: "egraph-guider",
          description: "Press <strong>play</strong> to hear a special message from " + Egraphs.page.signerName,
          id: "first",
          offset: {
            top: 20,
            left: -3
          },
          position: 12,
          title: Egraphs.page.signerName + " has a message for you!",
          width: 500,
          xButton: true
        }).show();

        /**
         * Since the 360 player injects html at some point
         * after document.ready(), we bind to it's container since it is always available.
        **/
        $("#360player").click(function() {
          guiders.hideAll();
        });

        // Bind to the xButton on the top right to hide the guider.
        $(".xButton").click(function() {
          guiders.hideAll();
        });
      }
    }
  };
});