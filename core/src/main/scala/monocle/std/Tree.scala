package monocle.std

import monocle.function._
import monocle.{Iso, Lens}

import scalaz.Tree
import scala.annotation.tailrec
import scala.collection.immutable.Stream.Empty


object tree extends TreeFunctions with TreeInstances

trait TreeFunctions {

  def rootLabel[A]: Lens[Tree[A], A] =
    Lens[Tree[A], A](_.rootLabel)(l => tree => Tree.node(l, tree.subForest))

  def subForest[A]: Lens[Tree[A], Stream[Tree[A]]] =
    Lens[Tree[A], Stream[Tree[A]]](_.subForest)(children => tree => Tree.node(tree.rootLabel, children))

  def leftMostLabel[A]: Lens[Tree[A], A] = {

    @tailrec
    def _get(tree: Tree[A]): A = tree.subForest match {
      case Empty    => tree.rootLabel
      case x #:: xs => _get(x)
    }

    def _set(newLeaf: A)(tree: Tree[A]): Tree[A] = tree.subForest match {
      case Empty => Tree.leaf(newLeaf)
      case xs    => Tree.node(tree.rootLabel, headMaybe[Stream[Tree[A]], Tree[A]].modify(_set(newLeaf))(xs) )
    }

    Lens(_get)(_set)
  }

  def rightMostLabel[A]: Lens[Tree[A], A] = {

    @tailrec
    def _get(tree: Tree[A]): A = tree.subForest match {
      case Empty => tree.rootLabel
      case xs    => _get(xs.last)
    }

    def _set(newLeaf: A)(tree: Tree[A]): Tree[A] = tree.subForest match {
      case Empty => Tree.leaf(newLeaf)
      case xs    => Tree.node(tree.rootLabel, lastMaybe[Stream[Tree[A]], Tree[A]].modify(_set(newLeaf))(xs) )
    }

    Lens(_get)(_set)
  }

}

trait TreeInstances {

  implicit def treeEach[A]: Each[Tree[A], A] = Each.traverseEach[Tree, A]

  implicit def treeReverse[A]: Reverse[Tree[A], Tree[A]] = new Reverse[Tree[A], Tree[A]] {
    def reverse = Iso[Tree[A], Tree[A]](reverseTree)(reverseTree)
    private def reverseTree(tree: Tree[A]): Tree[A] = Tree.node(tree.rootLabel, tree.subForest.reverse.map(reverseTree))
  }

}
