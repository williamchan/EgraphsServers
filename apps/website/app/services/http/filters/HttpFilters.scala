package services.http.filters

import com.google.inject.Inject

// TODO: PLAY20 migration. Comment this.
class HttpFilters @Inject()(
  val requireAccountEmail: RequireAccountEmail,
  val requireAdministratorLogin: RequireAdministratorLogin,
  val requireApplicationId: RequireApplicationId,
  val requireAuthenticatedAccount: RequireAuthenticatedAccount,
  val requireAuthenticityTokenFilter: RequireAuthenticityTokenFilter,
  val requireCelebrityAndProductUrlSlugs: RequireCelebrityAndProductUrlSlugs,
  val requireCelebrityId: RequireCelebrityId,
  val requireCelebrityPublishedAccess: RequireCelebrityPublishedAccess,
  val requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  val requireCustomerLogin: RequireCustomerLogin,
  val requireCustomerUsername: RequireCustomerUsername,
  val requireEgraphId: RequireEgraphId,
  val requireInventoryBatchId: RequireInventoryBatchId,
  val requireOrderId: RequireOrderId,
  val requireOrderIdOfCelebrity: RequireOrderIdOfCelebrity,
  val requirePrintOrderId: RequirePrintOrderId,
  val requireProductId: RequireProductId,
  val requireProductPublishedAccess: RequireProductPublishedAccess,
  val requireProductUrlSlug: RequireProductUrlSlug,
  val requireSessionId: RequireSessionId
)
