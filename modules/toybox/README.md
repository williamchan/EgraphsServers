# Egraphs ToyBox Module

ToyBox provides a simple way to lock down Play 2.0 applications. It is for making an application private -- requiring log in to access. It is not for handling normal user authentication. 

ToyBox is the bouncer to the party that is your Play application, which is most likely in beta or for internal use.

## Use:

To use ToyBox in an application:

1. Add Build dependency on ToyBoxBuild.main

2. Mix ToyBox into your `Global` object (by default, `object Global` in the default package). Make sure to call `super.onRouteRequest` from your implementation of `onRouteRequest` if it's been overridden.

3. Define `loginPath: String` (ex: `/toybox/login`) and `assetsPath: String => Call` (ex: `routes.Assets.at`) in your Global object.

4. Configure through `application.conf` with the following keys within the toybox sub-configuration (e.g. toybox.[some key]=[some value]):
  * `username` -- username credential for login; defaults to the empty string
  * `password` -- password credential for login; required
  * `is-private` -- boolean flag for application privacy; defaults to private (true)
  * `initial-request-cookie` -- name for the cookie used to store the method and path of initial request for redirection upon log-in; defaults to "toybox-initial-request"
  * `auth-cookie` -- name for authentication cookie; defaults to "toybox-authenticated"
  * `auth-timeout` -- maximum age of authentication cookie before expiring in integer number of seconds; defaults to 40 minutes
  * `auth-path` -- authentication cookie path; defaults to /
  * `auth-domain` -- authentication cookie domain; defaults to null (is wrapped in Option type in app)
  * `ipad-header` -- header under which to store ipad secret for ipad authorization
  * `ipad-secret` -- header value for ipad authentication (currently checks for secret, but could use secret to encode ip address and use THAT as the value)

### Minimal application.conf's
For all ToyBoxed applications, you should follow steps #1-3; the following information is regarding step #4 and setting an application as private or public. 

A private ToyBoxed application must have a password configured. `is-private` can be left out, or set to true (e.g. `toybox.password=somepassword`).

A public ToyBoxed application only needs `is-private` set to false (e.g. `toybox.is-private=false`).


Email kevin@egraphs.com for questions.
