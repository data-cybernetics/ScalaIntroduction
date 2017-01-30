package com.datacybernetics.qml2017.util

/*
 Additionally to abstract classes, scala knows the very powerful 'trait' which in
 first approximation is a Java interface.

 Upon a closer look you will realize that the 'trait' actually somewhat allows for
 multiple inheritance. This is a language feature that I have not completely grasped, so I
 encourage you to take a tutorial on traits!
 */
class TraitShowcase extends Showcase with AnotherShowcase {
  def sayHi() = {
    "Hi"
  }
}

trait Showcase {
  self: TraitShowcase =>

  def sayHiWithUser(user: String) = {
    s"${self.sayHi()} $user."
  }
}

trait AnotherShowcase {
  self: TraitShowcase =>

  def sayHiWithUserAge(user: String, age: Int) = {
    s"${self.sayHi()} $user, I think you are $age years old."
  }
}
