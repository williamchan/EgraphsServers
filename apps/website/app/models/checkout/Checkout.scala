package models.checkout


case class Checkout(lineItemTypes: IndexedSeq[LineItemType[_]]) {

  def add(lineItemType: LineItemType[_]): Checkout = {
    this.copy(lineItemTypes = lineItemTypes :+ lineItemType)
  }

  lazy val lineItems: IndexedSeq[LineItem[_]] = {
    case class ResolutionPass(items: IndexedSeq[LineItem[_]], unresolved: IndexedSeq[LineItemType[_]]) {
      def isComplete: Boolean = unresolved.isEmpty
    }

    def executePass(passToResolve: ResolutionPass): IndexedSeq[LineItem[_]] = {
      if (passToResolve.isComplete) {
        passToResolve.items
      } else {
        val resolvedPass = passToResolve.unresolved.foldLeft(passToResolve) { (oldPass, nextItemType) =>
          val itemTypesSansCurrent = oldPass.unresolved.filter(_ != nextItemType)

          nextItemType.lineItems(oldPass.items, itemTypesSansCurrent) match {
            case Some(newLineItems) => oldPass.copy(newLineItems, itemTypesSansCurrent)
            case None => oldPass
          }
        }

        // Check to make sure we're not in a circular dependency loop
        assert(
          resolvedPass.unresolved.length != passToResolve.unresolved.length,
          "Attempt to resolve LineItemTypes to LineItems failed to resolve even one: " +
            passToResolve.items +
            "\n\n" +
            passToResolve.unresolved
        )

        executePass(resolvedPass)
      }
    }

    val initialPass = ResolutionPass(IndexedSeq(), lineItemTypes)

    executePass(initialPass)
  }

  lazy val flattenedLineItems: IndexedSeq[LineItem[_]] = {
    for (lineItem <- lineItems; flattened <- lineItem.flatten) yield flattened
  }



  // TODO(SER-499): totals and json-ify
  // def subtotal: Money
  // def total: Money
  // def toJson: String


}
