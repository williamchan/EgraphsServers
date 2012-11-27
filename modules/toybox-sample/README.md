Sample project for ToyBox
=========================

This can be deleted after code review or left if it seems helpful.

For minimum configuration to use ToyBox, see:

1. ToyBoxSampleBuild.scala -- added dependency on ToyBox's build
2. app/controllers/MyToyBox.scala -- mixed in ToyBox and define routes to MyToyBox's getLogin and postLogin methods
3. conf/routes -- added routes to getLogin and postLogin
4. conf/application.conf -- configure MyToyBox as the global object and set username (dutchess) and password (934texas)


Overall, pretty painless!