package com.datacybernetics.qml2017.util

/*
 As known in java, you can define an abstract class from which other classes
 can be derived. We do this here and hardcode the characters, by using case classes, which
 will help us use them in pattern matching.
  */
abstract class Letter(val char: Char)
case class LetterH() extends Letter('H')
case class LetterE() extends Letter('E')
case class LetterL() extends Letter('L')
case class LetterO() extends Letter('O')
case class LetterW() extends Letter('W')
case class LetterR() extends Letter('R')
case class LetterD() extends Letter('D')
case class LetterSpace() extends Letter(' ')