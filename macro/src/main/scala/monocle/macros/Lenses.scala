package monocle.macros

import monocle.macros.internal.MacrosCompatibility

class Lenses(prefix: String = "") extends scala.annotation.StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro LensesImpl.annotationMacro
}

private[macros] object LensesImpl extends MacrosCompatibility {

  def annotationMacro(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    val LensesTpe = createTypeName(c)("Lenses")
    val prefix = c.macroApplication match {
      case Apply(Select(Apply(Select(New(Ident(LensesTpe)), t), args), _), _) if t == getTermNames(c).CONSTRUCTOR => args match {
        case Literal(Constant(s: String)) :: Nil => s
        case _ => ""
      }
      case _ => ""
    }

    def lenses(tpname: TypeName, paramss: List[List[ValDef]]): List[Tree] = {
      paramss.head map { param =>
        val lensName = createTermName(c)(prefix + param.name.decodedName)
        q"""val $lensName = monocle.macros.internal.Macro.mkLens[$tpname, ${param.tpt}](${param.name.toString})"""
      }
    }

    val result = annottees map (_.tree) match {
      case (classDef @ q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }")
        :: Nil if mods.hasFlag(Flag.CASE) =>
        val name = tpname.toTermName
        q"""
         $classDef
         object $name {
           ..${lenses(tpname, paramss)}
         }
         """
      case (classDef @ q"$mods class $tpname[..$tparams] $ctorMods(...$paramss) extends { ..$earlydefns } with ..$parents { $self => ..$stats }")
        :: q"object $objName {..$objDefs}"
        :: Nil if mods.hasFlag(Flag.CASE) =>
        q"""
         $classDef
         object $objName {
           ..${lenses(tpname, paramss)}
           ..$objDefs
         }
         """
      case _ => c.abort(c.enclosingPosition, "Invalid annotation target: must be a case class")
    }

    c.Expr[Any](result)
  }
}
