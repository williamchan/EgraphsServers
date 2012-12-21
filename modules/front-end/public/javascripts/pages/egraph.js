/* Scripting for the egraph page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {
      guiders.createGuider({
        attachTo: ".sm2-360btn",
        buttons: [{name: "Close", onclick: guiders.hideAll}],
        classString: "egraph-guider",
        description: "Hello World!",
        id: "first",
        overlay: true,
        position: 6,
        title: "You can also advance guiders from custom event handlers.",
        width: 450
      }).show();
    }
  };
});