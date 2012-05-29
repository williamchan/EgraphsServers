Front-end View Tags
===================

Bite-sized fragments of functionality usable from other templates.

@safeForm -- Create CSRF-protected forms
----------------------------------------

Creates a `<form>` tag that protects against Cross-Site Request Forgery
(CSRF) vulnerabilities by including a hidden `authenticityToken` html
`<input>` tag that is tied to the current user's session. 

It requires an implicit play.mvc.Scope.Session object, which can be provided
by any Controller.

Provide the usual form attributes in scala tuple syntax rather than
html syntax.

Usage:

```html
@(implicit session: play.mvc.Scope.Session)

@import views.frontend.tags

<html>
  <body>
    <!-- Put in a safe form, specifying the form attributes with scala tuple syntax --> 
    @tags.safeForm("method" -> "GET", action -> "/login") {
      username: <input type="text" name="fname" />
      password: <input type="password" name="fname" />
      <input type="submit" value="Submit" />
    }
  </body>
</html>
```
