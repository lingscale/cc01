package lingscale.cc01.config

abstract class Field[T] private (val default: Option[T])
{
  def this() = this(None)
  def this(default: T) = this(Some(default))
}

abstract class View {
  final def apply[T](pname: Field[T]): T = apply(pname, this)
  final def apply[T](pname: Field[T], site: View): T = {
    val out = find(pname, site)
    require (out.isDefined, s"Key ${pname} is not defined in Parameters")
    out.get
  }

  protected[config] def find[T](pname: Field[T], site: View): Option[T]
}

abstract class Parameters extends View {
  protected[config] def find[T](pname: Field[T], site: View): Option[T]
}

object Parameters {
  def apply(f: (View) => PartialFunction[Any,Any]): Parameters = new PartialParameters(f)
}

class Config(p: Parameters) extends Parameters {
  def this(f: (View) => PartialFunction[Any,Any]) = this(Parameters(f))

  protected[config] def find[T](pname: Field[T], site: View) = p.find(pname, site)

  def toInstance = this
}

// Internal implementation:

private class PartialParameters(f: (View) => PartialFunction[Any,Any]) extends Parameters {
  protected[config] def find[T](pname: Field[T], site: View) = {
    val g = f(site)
    if (g.isDefinedAt(pname)) Some(g.apply(pname).asInstanceOf[T]) else pname.default
  }
}
