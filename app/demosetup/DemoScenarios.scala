package demosetup

import models.{Account, Celebrity}
import libs.Blobs
import Blobs.Conversions._


class DemoScenarios extends DeclaresDemoScenarios {
  val demoCategory = "Demo Preparation"
  val celebrityPageCategory = "Celebrity Page"
  val productPageCategory = "Product Page"
  val orderConfirmationPageCategory = "Order Confirmation Page"
  val egraphPageCategory = "Egraph Page"

  //  toDemoScenarios add DemoScenario(
  //  "Create all demo celebrities",
  //  demoCategory,
  //  "", {
  //    () =>
  //      DemoScenario.clearAll()
  //      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
  //      createCelebrity("Clayton", "Kershaw", "ckershaw@egraphs.com", "claytonkershaw", true)
  //      createCelebrity("Don", "Mattingly", "dmattingly@egraphs.com", "donmattingly", true)
  //      createCelebrity("David", "Ortiz", "dortiz@egraphs.com", "davidortiz")
  //      createCelebrity("Dustin", "Pedroia", "dpedroia@egraphs.com", "dustinpedroia")
  //      createCelebrity("Hanley", "Ramirez", "hramirez@egraphs.com", "hanleyramirez")
  //      createCelebrity("Evan", "Longoria", "elongoria@egraphs.com", "evanlongoria")
  //      createCelebrity("Prince", "Fielder", "pfielder@egraphs.com", "princefielder")
  //      createCelebrity("Curtis", "Granderson", "cgranderson@egraphs.com", "curtisgranderson")
  //      createCelebrity("Nick", "Swisher", "nswisher@egraphs.com", "nickswisher")
  //      createCelebrity("Barry", "Bonds", "bbonds@egraphs.com", "barrybonds")
  //      createCelebrity("Ryan", "Braun", "rbraun@egraphs.com", "ryanbraun")
  //      createCelebrity("David", "Price", "dprice@egraphs.com", "davidprice")
  //  }
  //  )

  toDemoScenarios add DemoScenario(
  "Create Gabe Kapler",
  demoCategory,
  "", {
    () =>
      createCelebrity("Gabe", "Kapler", "gabe@egraphs.com", "gabekapler")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 16 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Hendricks Sports Management meeting. Creates:
  Clayton Kershaw (ckershaw@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Clayton", "Kershaw", "ckershaw@egraphs.com", "claytonkershaw", true)
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 17 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Ray Schulte meeting. Creates:
  Don Mattingly (dmattingly@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Don", "Mattingly", "dmattingly@egraphs.com", "donmattingly", true)
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 18 2012 agent meetings",
  demoCategory,
  """
  Prepares data for SFX meeting. Creates:
  David Ortiz (dortiz@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("David", "Ortiz", "dortiz@egraphs.com", "davidortiz")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 19 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Aces, Sam and Seth Levinson meeting. Creates:
  Dustin Pedroia (dpedroia@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Dustin", "Pedroia", "dpedroia@egraphs.com", "dustinpedroia")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 20 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Wasserman Media Group meeting. Creates:
  Hanley Ramirez (hramirez@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Hanley", "Ramirez", "hramirez@egraphs.com", "hanleyramirez")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 24 2012 agent meetings",
  demoCategory,
  """
  Prepares data for TWC Sports Management and Boras Corporation meetings. Creates:
  Evan Longoria (elongoria@egraphs.com/derp),
  Prince Fielder (pfielder@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Evan", "Longoria", "elongoria@egraphs.com", "evanlongoria")
      createCelebrity("Prince", "Fielder", "pfielder@egraphs.com", "princefielder")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 25 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Matt Brown meeting. Creates:
  Curtis Granderson (cgranderson@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Curtis", "Granderson", "cgranderson@egraphs.com", "curtisgranderson")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 26 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Dan Lozano meeting. Creates:
  Nick Swisher (nswisher@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Nick", "Swisher", "nswisher@egraphs.com", "nickswisher")
  }
  )

  toDemoScenarios add DemoScenario(
  "Jan 27 2012 agent meetings",
  demoCategory,
  """
  Prepares data for Beverly Hills Sports Council and CAA meetings. Creates:
  Barry Bonds (bbonds@egraphs.com/derp),
  Ryan Braun (rbraun@egraphs.com/derp)
  """, {
    () =>
      createCelebrity("Barry", "Bonds", "bbonds@egraphs.com", "barrybonds")
      createCelebrity("Ryan", "Braun", "rbraun@egraphs.com", "ryanbraun")
  }
  )

  private def createCelebrity(firstName: String, lastName: String, email: String, s3ResourceId: String, productAOnly: Boolean = false) {
    println("Creating Celebrity " + email + " ...")

    val profile = "demo/" + s3ResourceId + "/" + s3ResourceId + "-profile.jpg"
    val productA = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-a.jpg"
    val productB = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-b.jpg"

    val celebrity = Celebrity(
      firstName = Some(firstName),
      lastName = Some(lastName),
      publicName = Some(firstName + " " + lastName),
      description = Some("Help me... help YOU...")
    ).save()

    if (!productAOnly) {
      celebrity.saveWithProfilePhoto(Blobs.getStaticResource(profile).get.asByteArray)
    }

    Account(email = email,
      celebrityId = Some(celebrity.id)
    ).withPassword("derp").right.get.save()

    celebrity.newProduct.copy(
      priceInCurrency = 50,
      name = firstName + "'s Product A",
      description = "Buy my eGraph A!"
    ).save().withPhoto(Blobs.getStaticResource(productA).get.asByteArray).save()

    if (!productAOnly) {
      celebrity.newProduct.copy(
        priceInCurrency = 100,
        name = firstName + "'s Product B",
        description = "Buy my eGraph B!"
      ).save().withPhoto(Blobs.getStaticResource(productB).get.asByteArray).save()
    }
  }

  //  toDemoScenarios add DemoScenario(
  //  "Testing",
  //  demoCategory,
  //  """
  //  Testing
  //  """, {
  //    () =>
  //      testBlobs("davidortiz")
  //      testBlobs("dustinpedroia")
  //      testBlobs("hanleyramirez")
  //      testBlobs("evanlongoria")
  //      testBlobs("princefielder")
  //      testBlobs("curtisgranderson")
  //      testBlobs("nickswisher")
  //      testBlobs("barrybonds")
  //      testBlobs("ryanbraun")
  //      testBlobs("gabekapler")
  //      testBlobs("davidprice")
  //
  //  }
  //  )
  //
  //  private def testBlobs(s3ResourceId: String) {
  //    println(s3ResourceId + "...")
  //    val profile = "demo/" + s3ResourceId + "/" + s3ResourceId + "-profile.jpg"
  //    val productA = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-a.jpg"
  //    val productB = "demo/" + s3ResourceId + "/" + s3ResourceId + "-product-b.jpg"
  //    println("profile: " + Blobs.getStaticResource(profile))
  //    println("product-a: " + Blobs.getStaticResource(productA))
  //    println("product-b: " + Blobs.getStaticResource(productB))
  //    println()
  //  }

}