/* Scripting for the egraph page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {
      guiders.createGuider({
        buttons: [{name: "Close"},
                  {name: "Next"}],
        classString: "egraph-guider",
        description: "Please follow these messages to find out about all the cool features your egraph has to offer.",
        id: "first",
        next: "second",
        overlay: true,
        title: "Welcome to your egraph!"
      }).show();

      guiders.createGuider({
        attachTo: ".sm2-360btn",
        buttons: [{name: "Close"},
                  {name: "Next"}],
        classString: "egraph-guider",
        description: "Click play to hear a personalized audio message from your star.",
        id: "second",
        next: "third",
        position: 12,
        title: "Your personalized audio message",
        width: 500
      })

      guiders.createGuider({
        attachTo: ".social",
        buttons: [{name: "Close", onclick: guiders.hideAll}],
        classString: "egraph-guider",
        description: "Use the buttons below to share your egraph with your family and friends.",
        id: "third",
        position: 12,
        title: "Share your egraph",
        width: 350
      })
    }
  };
});