# Egraphs ToyBox Module

ToyBox provides a simple way to lock down Play 2.0 applications. It is for
making an application private -- requiring log in to access. It is not for
handling normal user authentication. 

ToyBox is the bouncer to the party that is your Play application that is 
most likely in beta or for internal use.

## Use:

To use ToyBox in an application:
1. Make sure the Global object of your project, if it exists, is in a package
2. Mix ToyBox into your Global object. (Make sure to call super.onRouteRequest from your implementation of onRouteRequest if it's been overridden.)
4. Add routes to your Global.getLogin and Global.postLogin
5. Add abstract members of ToyBox to your Global object
6. Configure through application.conf (see ToyBoxBase.scala for details) with the following keys within the toybox sub-configuration (e.g. toybox.[some key]=[some value]):
* username -- username credential for login; defaults to the empty string
* password -- password credential for login; required
* is-private -- boolean flag for application privacy; defaults to private (true)
* initial-request-cookie -- name for the cookie used to store the method and path of initial request for redirection upon log-in; defaults to "toybox-initial-request"
* auth-cookie -- name for authentication cookie; defaults to "toybox-authenticated"
* auth-timeout -- maximum age of authentication cookie before expiring in integer number of seconds; defaults to 40 minutes
* auth-path -- authentication cookie path; defaults to /
* auth-domain -- authentication cookie domain; defaults to null (is wrapped in Option type in app)

Email kevin@egraphs.com for questions.