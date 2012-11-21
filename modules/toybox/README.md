Egraphs ToyBox Module
=====================

ToyBox provides a simple way to lock down Play 2.0 applications. It is for
making an application private -- requiring log in to access. It is not for
handling normal user authentication. 

ToyBox is the bouncer to the party that is your Play application that is 
most likely in beta or for internal use.

To use ToyBox in an application:
  0) Make sure the Global object of your project, if it exists, is in a package

  1) Mix ToyBox into your Global object
    1.5) Call super.onRouteRequest from your implementation of onRouteRequest
         if it's been overridden

  2) Add routes to your Global.getLogin and Global.postLogin

  3) Add abstract members of ToyBox to your Global object

  4) Configure through application.conf (see ToyBoxBase.scala for details)


For more infortmation, see 
* [Design topic](https://egraphs.atlassian.net/wiki/pages/viewpage.action?pageId=15269913)
* [Issue backlog](https://egraphs.atlassian.net/browse/SER-425)

Email kevin@egraphs.com for questions.