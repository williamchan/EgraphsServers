package egraphs.playutils

/**DIY exhaustiveness-checking enum type. See https://gist.github.com/1057513 */
trait Enum {

  import java.util.concurrent.atomic.AtomicReference

  type EnumVal <: Value //This is a type that needs to be found in the implementing class

  private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values. Using AtomicReference due to Concurrency paranoia

  //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
  private final def addEnumVal(newVal: EnumVal): Int = {
    import _values.{ get, compareAndSet => CAS }

    val oldVec = _values.get
    val newVec = oldVec :+ newVal
    if ((get eq oldVec) && CAS(oldVec, newVec)) newVec.indexWhere(_ eq newVal) else addEnumVal(newVal)
  }

  def values: Vector[EnumVal] = _values.get //Here you can get all the enums that exist for this type
  /**
   * Returns an Option[EnumVal] if the Enum has a corresponding mapping form string to enumval
   * @param name String name of enum
   * @return
   */
  def apply(name: String): Option[EnumVal] = {
    values.filter(en => en.name == name).headOption
  }

  //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
  protected trait Value extends Serializable {
    //Enforce that no one mixes in Value in a non-EnumVal type
    self: EnumVal =>
    final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

    def name: String //All enum values should have a name

    //And that name is used for the toString operation
    override def toString = name

    override def equals(other: Any) = {
      other match {
        case thisType: Value =>
          thisType.name == this.name

        case otherType =>
          this eq other.asInstanceOf[AnyRef]
      }
    }

    override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
  }

}