package net.liftweb.json

import _root_.org.scalacheck._
import _root_.org.scalacheck.Prop.forAll
import _root_.org.specs.Specification
import _root_.org.specs.runner.{Runner, JUnit}
import _root_.org.specs.ScalaCheck

class ParserTest extends Runner(ParserSpec) with JUnit
object ParserSpec extends Specification with JValueGen with ScalaCheck {
  import JsonAST._
  import JsonParser._

  "Any valid json can be parsed" in {
    val parsing = (json: JValue) => { parse(JsonDSL.pretty(render(json))); true }
    forAll(parsing) must pass
  }

  implicit def arbJValue: Arbitrary[JValue] = Arbitrary(genObject)
}
