/* Scripting for the egraph classic page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {
      // This binding cannot occur until the soundmanager js is loaded.
      // There is no way to decode that so the selector is included in the function.
      var playSound = function() {
        $(".sm2-360btn").click();
        guiders.hideAll();
      };

      $(document).ready(function () {
      /**
       * Create a guider to highlight the Play button on the Egraph classic page.
       **/
        if(Egraphs.page.isPromotional === true) {
          guiders.createGuider({
            attachTo: "#360player",
            classString: "egraph-classic-guider",
            description: "<strong id='clickortouch'>Click or touch</strong> to hear a special message from " + Egraphs.page.signerName,
            id: "first",
            offset: {
              top: 20,
              left: 4
            },
            position: 12,
            title: Egraphs.page.signerName + " has a message for you!",
            width: 500,
            xButton: true
          }).show();

          $("#clickortouch").click(playSound);
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
      });
    }
  };
});