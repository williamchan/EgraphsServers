package services.http.forms.purchase

import utils.EgraphsUnitTest
import services.AppConfig

class PurchaseFormValidationsTests extends EgraphsUnitTest {

  "all required fields" should "be required" in {
    val empties = List(List.empty[String], List(""))

    for (empty <- empties) {
      val validations = validationsFor(empty)

      val shouldBeRequired = List(
        validations.isProductId,
        validations.isRecipientChoice,
        validations.isName,
        validations.isWrittenMessageRequest,
        validations.isPrintingOption
      )

      for (test <- shouldBeRequired) {
        test match {
          case Left(error) =>
            error.description should be (PurchaseFormChecks.requiredError)

          case Right(_) =>
            fail("The field should have been required")
        }
      }
    }
  }

  def validationsFor(toValidate: Iterable[String]) = {
    val fact = AppConfig.instance[PurchaseFormChecksFactory]

    fact(toValidate)
  }
}
