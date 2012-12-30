package services

import scalaz.Lens

/** See `asMember` docs in [[services.MemberLens.Conversions.MemberLensConversion]] */
class MemberLens[A, B](private val self: A, private val lens: Lens[A, B]) {
  def get: B = lens.get(self)
  def apply(): B = lens.get(self)
  def set(newValue: B) = lens.set(self, newValue)
}

object MemberLens {

  def apply[A, B](self: A)(getter: A => B, setter: (A,B) => A) = new MemberLens[A,B](self,
    new Lens[A,B]( getter, setter )
  )

  object Conversions {
    implicit def lensToMemberLensConversion[A, B](lens: Lens[A, B]): MemberLensConversion[A, B] = {
      new MemberLensConversion(lens)
    }

    implicit def memberLensToLens[A, B](memberLens: MemberLens[A, B]): Lens[A, B] = {
      memberLens.lens
    }

    class MemberLensConversion[A, B](lens: Lens[A, B]) {
      /**
       * Gives OO property-access semantics to objects that contain scalaz lenses. Most useful for when
       * you want to write generic code that manipulates a delegated object (for example,
       * [[models.checkout.LineItemType]]s that use [[models.checkout.LineItemTypeEntity]] for persistence)
       *
       * Trivial example:
       *
       * {{{
       *   import scalaz.Lens
       *   import services.MemberLens.Conversions._
       *
       *   object MemberLensExample {
       *     case class Address(street: String, city: String)
       *     case class Person(name: String, address: Address) {
       *       val city = Lens[Person, String](
       *         get= (person) => person.address.city,
       *         set= (city, person) => person.copy(address=person.address.copy(city=city)
       *       ).asMember
       *     }
       *
       *     def usePerson = {
       *       val person = Person("Herp Derpson", Address("123 Derp Street", "Seattle"))
       *       val cajunPerson = person.city.set("New Orleans")
       *
       *       person.city() == "Seattle" // true. Could also be person.city.get
       *       cajunPerson.city() == "New Orleans" // true
       *     }
       *   }
       * }}}
       *
       */
      def asMember(instance: A) = new MemberLens(instance, lens)
    }
  }
}