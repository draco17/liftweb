package net.liftweb.util
import _root_.org.specs.runner._
import _root_.org.specs._
import _root_.java.lang.reflect.{Method}
import _root_.org.scalacheck.Arbitrary
import _root_.org.scalacheck.{Prop, Gen}
import _root_.org.scalacheck.Gen._
import _root_.org.specs.ScalaCheck
import common._

class ClassHelpersSpecTest extends Runner(ClassHelpersSpec) with JUnit
object ClassHelpersSpec extends Specification with ClassHelpers with ControlHelpers with StringGenerators with ScalaCheck {
  "the findType function" should {
    "return a Full can with the found class when given the type, the name, and a list of packages to conform to" in {
      findType[_root_.java.util.List[Object]]("ArrayList", List("java.util")) must_== Full(classOf[_root_.java.util.ArrayList[Object]])
    }
    "return an Empty can if the class cannot be coerced to the expected type" in {
      findType[String]("ClassHelpersSpecTest", List("net.liftweb.util")) must_== Empty
    }
  }
  "the findClass function" should {
    "return a Full can with the found class when given the name and package" in {
      findClass("ClassHelpersSpecTest", List("net.liftweb.util")) must_== Full(classOf[ClassHelpersSpecTest])
    }
    "return a Full can with the found class when given the name and package, with an underscored name instead of CamelCased" in {
      findClass("class_helpers_spec_test", List("net.liftweb.util")) must_== Full(classOf[ClassHelpersSpecTest])
    }
    "return a Full can with the found class when given the name and a list of packages" in {
      findClass("ClassHelpersSpecTest", List("net.liftweb.util", "other.package")) must_== Full(classOf[ClassHelpersSpecTest])
    }
    "return a Full can with the found class when given the name, a list of packages and a target type to conform to" in {
      findClass("ArrayList", List("java.util"), classOf[_root_.java.util.List[Object]]) must_== Full(classOf[_root_.java.util.ArrayList[Object]])
    }
    "return an Empty can if no class is found given a name and package" in {
      findClass("ClassHelpersSpecTest", List("net.liftweb.nothere")) must_== Empty
    }
    "return an Empty can if the class cannot be coerced to the expected type" in {
      findClass("ClassHelpersSpecTest", List("net.liftweb.util"), classOf[String]) must_== Empty
    }
  }
  "the findClass function" can {
    "return a Full can with the found class when given a list of names and corresponding packages" in {
      findClass(List(("wrong name", List("net.liftweb.util", "other.package")),
                     ("ClassHelpersSpecTest", List("net.liftweb.util", "other.package")))) must_== Full(classOf[ClassHelpersSpecTest])
    }
    "use a list of modifiers functions to try to modify the original name in order to find the class" in {
      findClass("classHelpersSpecTest", List("net.liftweb.util"), List((n: String) => n.capitalize)) must_== Full(classOf[ClassHelpersSpecTest])
    }
  }
  "The callableMethod_? function" should {
    "return true if the method is public and has no parameters" in {
      val publicParameterLess = classOf[String].getMethod("length")
      callableMethod_?(publicParameterLess) must beTrue
    }
    "return false if the method is public and has parameters" in {
      val publicWithParameters = classOf[String].getMethod("indexOf", classOf[String])
      callableMethod_?(publicWithParameters) must beFalse
    }
    "return false if the method is private" in {
      val privateMethod = classOf[_root_.java.util.ArrayList[Object]].getDeclaredMethod("readObject", classOf[_root_.java.io.ObjectInputStream])
      callableMethod_?(privateMethod) must beFalse
    }
    "return false if the method is null" in {
      callableMethod_?(null) must beFalse
    }
  }
  "The containsClass function" should {
    "return false if the list to match is null or empty" in {
      containsClass(classOf[String], null) must beFalse
      containsClass(classOf[String], Nil) must beFalse
    }
    "return false if the list to match doesn't contain any class assignable by the tested class" in {
      containsClass(classOf[String], List(classOf[Float], classOf[Integer])) must beFalse
    }
  }
  "The camelCase function" should {
    "CamelCase a name which is underscored, removing each underscore and capitalizing the next letter" in {
      def previousCharacterIsUnderscore(name: String, i: Int) = i > 1 && name.charAt(i - 1) == '_'
      def underscoresNumber(name: String, i: Int) = if (i == 0) 0 else name.substring(0, i).toList.count(_ == '_')
      def correspondingIndexInCamelCase(name: String, i: Int) = i - underscoresNumber(name, i)
      def correspondingCharInCamelCase(name: String, i: Int): Char = camelCase(name).charAt(correspondingIndexInCamelCase(name, i))

      val doesntContainUnderscores = forAllProp(underscoredStrings)((name: String) => !camelCase(name).contains("_"))
      val isCamelCased = forAllProp(underscoredStrings) ((name: String) => {
        name.forall(_ == '_') && camelCase(name).isEmpty ||
        name.toList.zipWithIndex.forall { case (c, i) =>
          c == '_' ||
          correspondingIndexInCamelCase(name, i) == 0 && correspondingCharInCamelCase(name, i) == c.toUpperCase ||
          !previousCharacterIsUnderscore(name, i) && correspondingCharInCamelCase(name, i) == c ||
          previousCharacterIsUnderscore(name, i) && correspondingCharInCamelCase(name, i) == c.toUpperCase
       }
      })
      doesntContainUnderscores && isCamelCased must pass
    }
    "return an empty string if given null" in {
      camelCase(null) must_== ""
    }
    "leave a CamelCased name untouched" in {
      val camelCasedNameDoesntChange = forAllProp(camelCasedStrings){ (name: String) => camelCase(name) == name }
      camelCasedNameDoesntChange must pass
    }
  }
  "The camelCaseMethod function" should {
    "camelCase a name with the first letter being lower cased" in {
      val camelCasedMethodIsCamelCaseWithLowerCase = forAllProp(underscoredStrings){
        (name: String) =>
        camelCase(name).isEmpty && camelCaseMethod(name).isEmpty ||
        camelCaseMethod(name).toList.head.isLowerCase && camelCase(name) == camelCaseMethod(name).capitalize
      }
      camelCasedMethodIsCamelCaseWithLowerCase must pass
    }
  }
  "The unCamelCase function" should {
    "Uncamel a name, replacing upper cases with underscores" in {
      forAllProp(camelCasedStrings)((name: String) => camelCase(unCamelCase(name)) == name) must pass
    }
  }
  "The classHasControllerMethod function" should {
    "return true if the class has 'name' as a callable method" in {
      classHasControllerMethod(classOf[String], "length") must beTrue
    }
    "return false if the class doesn't have 'name' as a method" in {
      classHasControllerMethod(classOf[String], "isNotEmpty") must beFalse
    }
    "return false if the class has a method but it is not callable" in {
      classHasControllerMethod(classOf[_root_.java.util.ArrayList[Object]], "readObject") must beFalse
    }
    "return false if the class is null" in {
      classHasControllerMethod(null, "readObject") must beFalse
    }
  }
  "The invokeControllerMethod function" should {
    "return the result of calling the method on a new instance of the class" in {
      invokeControllerMethod(classOf[String], "length") must_== 0
    }
    "throw an exception when the method is not callable" in {
      invokeControllerMethod(classOf[String], "isNotEmpty") must throwA[NoSuchMethodException]
    }
    "throw an exception if the class is null" in {
      invokeControllerMethod(null, "length") must throwA[NullPointerException]
    }
  }
  "The invokeMethod function" should {
    "return a Failure if the class is null" in {
      invokeMethod(null, "", "length") must beLike { case Failure(_, _, _) => true }
    }
    "return a Failure if the instance is null" in {
      invokeMethod(classOf[String], null, "length") must beLike { case Failure(_, _, _) => true }
    }
    "return a Failure if the method name is null" in {
      invokeMethod(classOf[String], "", null) must beLike { case Failure(_, _, _) => true }
    }
    "return a Failure if the method doesnt exist on the class" in {
      invokeMethod(classOf[String], "", "isNotEmpty") must beLike { case Failure(_, _, _) => true }
    }
    "return a Full can with the result if the method exist on the class" in {
      invokeMethod(classOf[String], "", "length") must_== Full(0)
    }
    "return a Full can with the result if the method is an existing static method on the class" in {
      invokeMethod(classOf[_root_.java.util.Calendar], null, "getInstance").isEmpty must_== false
    }
    "throw an exception if the method throws an exception" in {
      class SpecificException extends Exception
      class TestSnippet { def throwException = throw new SpecificException  }
      val testSnippet = new TestSnippet
      invokeMethod(testSnippet.getClass, testSnippet, "throwException") must throwA[SpecificException]
    }
  }
  "The invokeMethod function" can {
    "call a method with its parameters" in {
      invokeMethod(classOf[String], "", "valueOf", Array("1")) must_== Full("1")
    }
    "call a method with its parameters and parameter types" in {
      invokeMethod(classOf[String], "", "valueOf", Array("c"), Array(classOf[String])) must_== Full("c")
    }
  }
  "The instantiate function" should {
    "return a full can if a class can be instantiated with a new instance" in {
      instantiate(classOf[String]) must_== Full("")
    }
    "return a failure if a class can not be instantiated with a new instance" in {
      instantiate(classOf[_root_.java.util.Calendar]) must beLike { case Failure(_, _, _) => true }
    }
  }
  "The createInvoker function" should {
    "return Empty if the instance is null" in {
      createInvoker("length", null) must_== Empty
    }
    "return a Full can with the function from Unit to a can containing the result of the method to invoke" in {
      createInvoker("length", "").open_!.apply().get must_== 0
    }
    "The invoker function will throw the cause exception if the method can't be called" in {
      createInvoker("get", "").open_!.apply must throwA[Exception]
    }
  }
}
trait StringGenerators {
  val underscoredStrings = for {length <- choose(0, 4)
                                string <- vectorOf(length, frequency((3, alphaChar), (1, elements('_'))))
                                } yield List.toString(string)

  val camelCasedStrings = for {length <- choose(0, 4)
         firstLetter <- alphaNumChar.map(_.toUpperCase)
         string <- vectorOf(length, frequency((3, alphaNumChar.map(_.toLowerCase)), (1, alphaNumChar.map(_.toUpperCase))))
        } yield List.toString(firstLetter :: string)
}
