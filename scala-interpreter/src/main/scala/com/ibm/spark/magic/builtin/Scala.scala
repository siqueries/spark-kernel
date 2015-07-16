package com.ibm.spark.magic.builtin

import com.ibm.spark.interpreter.{ExecuteAborted, ExecuteError}
import com.ibm.spark.kernel.interpreter.scala.{ScalaException, ScalaInterpreter}
import com.ibm.spark.kernel.protocol.v5.MIMEType
import com.ibm.spark.magic.dependencies.IncludeKernel
import com.ibm.spark.magic.{CellMagic, CellMagicOutput}

/**
 * Represents the magic interface to use the Scala interpreter.
 */
class Scala extends CellMagic with IncludeKernel {
  override def execute(code: String): CellMagicOutput = {
    val scala = Option(kernel.data.get("Scala"))

    if (scala.isEmpty || scala.get == null)
      throw new ScalaException("Scala is not available!")

    scala.get match {
      case scalaInterpreter: ScalaInterpreter =>
        val (_, output) = scalaInterpreter.interpret(code)
        output match {
          case Left(executeOutput) =>
            CellMagicOutput(MIMEType.PlainText -> executeOutput)
          case Right(executeFailure) => executeFailure match {
            case executeAborted: ExecuteAborted =>
              throw new ScalaException("Scala code was aborted!")
            case executeError: ExecuteError =>
              throw new ScalaException(executeError.value)
          }
        }
      case otherInterpreter =>
        val className = otherInterpreter.getClass.getName
        throw new ScalaException(s"Invalid Scala interpreter: $className")
    }
  }
}

