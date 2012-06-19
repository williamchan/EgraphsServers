A(ng)ular tags

To be used in conjuction with angular based forms. They contain various hooks specific to setting up form inputs.
Some tags are pretty specific to a form page that is row based, others are more general.

Example:

@import views.frontend.tags.ng

<html>
<body ng-controller="controller">
<form name="form">
@tags.ng.html.form_namevalue(model = "user.fullname", id = "fullname", title = "Name")
</form>
</body>
</html>

You will need to provide your own controller class.
http://angularjs.org/#/list