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
  val requireCelebrityPublished: RequireCelebrityPublished,
  val requireCelebrityUrlSlug: RequireCelebrityUrlSlug,
  val requireCustomerId: RequireCustomerId,
  val requireCustomerLogin: RequireCustomerLogin,
  val requireCustomerUsername: RequireCustomerUsername,
  val requireEgraphId: RequireEgraphId,
  val requireInventoryBatchId: RequireInventoryBatchId,
  val requireOrderIdOfCelebrity: RequireOrderIdOfCelebrity,
  val requirePrintOrderId: RequirePrintOrderId,
  val requireProductId: RequireProductId,
  val requireProductPublished: RequireProductPublished,
  val requireProductUrlSlug: RequireProductUrlSlug,
  val requireResetPasswordSecret: RequireResetPasswordSecret,
  val requireSessionId: RequireSessionId
)
