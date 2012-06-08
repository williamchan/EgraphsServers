//
// Models for use in the front-end project. Generally you shouldn't
// have to use these and should prefer using primitives (e.g. String,
// Int, Date, etc), but sometimes you have to manipulate lists of
// related data (e.g. lists of Product, written as Seq[Product]), in
// which case a small case class is a-OK.
//
// DO let your views drive the contents of these "models". Though that
// may seem strange, When the views get integrated into the main
// application then implicit conversions will be written to tern the
// *actual* model classes to and from the types you specify here.
//
// More about case classes: http://www.codecommit.com/blog/scala/case-classes-are-cool
//

package models.frontend

/**
 * An example of what a Product might look like. It has been namespaced to Frontend
 * to distinguish it from the canonical server-side Product.
 *
 * @param id the product's unique ID
 * @param name the product's name
 * @param price the product's price in dollars
 * @param description a long-form description of the product.
 * @param url link to the product in the Single Celebrity Storefront.
 */
case class ExampleFrontendProduct(
  id: String, 
  name: String, 
  price: String, 
  description: String = "", // You can optionally provide default values
  url: String = ""
)
