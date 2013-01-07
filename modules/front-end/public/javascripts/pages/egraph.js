/* Scripting for the egraph page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {
      guiders.createGuider({
        buttons: [{name: "Close", classString: "btn close-guider"},
                  {name: "Next", classString: "btn next"}],
        classString: "egraph-guider",
        closeOnEscape: true,
        description: "An egraph is a personalized message and this is a bunch of copy.",
        id: "first",
        next: "second",
        overlay: true,
        title: "Welcome to Egraphs",
        xButton: true
      }).show();

      guiders.createGuider({
        attachTo: ".sm2-360btn",
        buttons: [{name: "next", classString: "btn next"}
                  ],
        classString: "egraph-guider",
        description: "Press play to hear a message from Herp Derpson",
        highlight: ".sm2-360btn",
        id: "second",
        next: "third",
        position: 12,
        overlay: false,
        title: "Herp Derpson wanna say somethin'",
        width: 500
      });

      guiders.createGuider({
        attachTo: ".social",
        buttons: [{name: "Close", classString: "btn next", onclick: guiders.hideAll}],
        classString: "egraph-guider",
        description: "Use the buttons below to share your egraph with your family and friends.",
        id: "third",
        position: 12,
        title: "Share your egraph",
        width: 350
      });
    }
  };
});