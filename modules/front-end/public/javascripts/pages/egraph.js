/* Scripting for the egraph page */
define(["Egraphs", "libs/guiders"], function (Egraphs, guiders) {
	return {
    go: function () {
      guiders.createGuider({
        attachTo: "#360player",
        buttons: [{name: "Close", onclick: guiders.hideAll}],
        classString: "egraph-guider",
        closeOnEscape: true,
        description: "Click the play button to hear a special audio greeting!",
        id: "first",
        overlay: true,
        position: 9,
        title: "",
        width: 150,
        xButton: true
      }).show();
    }
  };
});