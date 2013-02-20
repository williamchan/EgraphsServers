package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import models.Account
import models.AccountStore
import models.Administrator
import models.AdministratorStore
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.status
import services.db.Schema
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import services.http.EgraphsSession
import utils.EgraphsUnitTest

@RunWith(classOf[JUnitRunner])
class RequireAdministratorLoginTests extends EgraphsUnitTest {
  val goodAdminId = 12093812408L
  val badAdminId = 1309174734L

  val administator = Administrator(id = goodAdminId, services = null)
  val adminAccount = Account(administratorId = Some(goodAdminId), _services = null)

  "filter" should "allow adminIds that have an account and administrator associated with them" in {
    val errorOrAccount = filterWithMocks.filter(goodAdminId)

    errorOrAccount should be(Right((administator, adminAccount)))
  }

  it should "not allow adminIds that have do not have an account associated with them" in {
    val errorOrAccount = filterWithMocks.filter(badAdminId)
    val result = errorOrAccount.toErrorOrOkResult

    status(result) should be(SEE_OTHER)
  }

  "form" should "require an adminId" in {
    val boundForm = filterWithMocks.form.bind(Map(EgraphsSession.Key.AdminId.name -> goodAdminId.toString))

    boundForm.value should be(Some(goodAdminId))
  }

  it should "fail if it does not have an adminId" in {
    val boundForm = filterWithMocks.form.bind(Map.empty[String, String])

    boundForm.value should be(None)
  }

  it should "fail if the adminId is not a positive number" in {
    val negativeAdminId = filterWithMocks.form.bind(Map(EgraphsSession.Key.AdminId.name -> -999L.toString))
    val zeroAdminId = filterWithMocks.form.bind(Map(EgraphsSession.Key.AdminId.name -> 0.toString))

    negativeAdminId.value should be(None)
    zeroAdminId.value should be(None)
  }

  private def filterWithMocks: RequireAdministratorLogin = {
    new RequireAdministratorLogin(mockAdministratorStore, mockAccountStore)
  }

  private def mockAdministratorStore = {
    // Problem in Scala 2.9.1 - This class only exists because Mockito has trouble mocking concrete classes, please replace in Scala 2.10.0 if possible.
    class MockableAdministratorStore(schema: Schema, accountStore: AccountStore) extends AdministratorStore(schema, accountStore) {
      override def findById(id: Long): Option[Administrator] = { None }
    }

    val administratorStore = mock[MockableAdministratorStore]

    administratorStore.findById(goodAdminId) returns Some(administator)
    administratorStore.findById(badAdminId) returns None

    administratorStore
  }

  private def mockAccountStore = {
    val accountStore = mock[AccountStore]

    accountStore.findByAdministratorId(goodAdminId) returns Some(adminAccount)
    accountStore.findByAdministratorId(badAdminId) returns None

    accountStore
  }
}