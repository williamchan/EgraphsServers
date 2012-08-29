Front-end View Tags
===================

Bite-sized fragments of functionality usable from other templates.


@safeForm -- Create CSRF-protected forms
----------------------------------------

Creates a `<form>` tag that protects against Cross-Site Request Forgery
(CSRF) vulnerabilities by including a hidden `authenticityToken` html
`<input>` tag that is tied to the current user's session. 

It requires an implicit play.api.mvc.Session object, which can be provided
by any Controller.

Provide the usual form attributes in scala tuple syntax rather than
html syntax.

Sample .scala.html template usage:

```html
@()(implicit session: play.api.mvc.Scope.Session)

@import views.frontend.tags

<html>
  <body>
    <!-- Put in a safe form, specifying the form attributes with scala tuple syntax --> 
    @tags.html.safeForm("method" -> "GET", "action" -> "/login") {
      username: <input type="text" name="fname" />
      password: <input type="password" name="fname" />
      <input type="submit" value="Submit" />
    }
  </body>
</html>
```

@site_header -- Website header
------------------------------

Drops the stock site header into the page at the location.


@site_footer -- Website footer
------------------------------

Drops the stock site footer into the page at the location.


