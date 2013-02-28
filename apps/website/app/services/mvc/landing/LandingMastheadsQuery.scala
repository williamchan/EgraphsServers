package services.mvc.landing

class LandingMastheadsQuery extends LandingMastheadQuerying {
  //
  // LandingMastheadsQuerying members
  //

  override protected def landingMastheadAgent = {
    LandingMastheadsAgent.singleton
  }

  override protected def landingMastheadUpdateActor = {
    UpdateLandingMastheadsActor.singleton
  }

}
