package utils

case class TestWebsiteControllers @Inject()(
  controllerMethod: ControllerMethod,
  postController: POSTControllerMethod,
  adminFilters: AdminRequestFilters,
  celebFilters: CelebrityAccountRequestFilters,
  mail: Mail,
  payment: Payment,
  orderQueryFilters: OrderQueryFilters,
  egraphQueryFilters: EgraphQueryFilters,
  inventoryBatchQueryFilters: InventoryBatchQueryFilters,
  dbSession: DBSession
  ) extends Controller with AllWebsiteEndpoints
