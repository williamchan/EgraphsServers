define(["Egraphs", "bootstrap/bootstrap-alert", "bootstrap/bootstrap-button", "libs/chosen/chosen.jquery.min", "services/forms"], function() {

  var confirmPost = function() {
    return confirm("This will cause delays for the customer. Are you sure you want to reject?");
  };

  return {
    go: function () {
      var mlbpaReject = $(".mlbpa-reject");
      mlbpaReject.submit(confirmPost);
    }
  };
});