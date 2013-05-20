package cleakka

import java.io.InputStream
import scala.util.control.Exception.allCatch

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Input, Output}

import com.twitter.bijection.Injection
import com.twitter.chill.KryoBijection

// Supports deserializing InputStream. See:
// https://github.com/twitter/chill/blob/develop/chill-scala/src/main/scala/com/twitter/chill/KryoBijection.scala
object InputStreamKryoInjection {
  // Create a default injection to use, 4KB init, max 16 MB
  private val kinject = new InputStreamKryoInjectionInstance(
      KryoBijection.getKryo, new Output(1 << 12, 1 << 24))

  def apply(obj: AnyRef) = kinject.synchronized { kinject(obj) }
  def invert(bytes: Array[Byte]) = kinject.synchronized { kinject.invert(bytes) }
  def invert(inputStream: InputStream) = kinject.synchronized { kinject.invert(inputStream) }
}

class InputStreamKryoInjectionInstance(kryo: Kryo, output: Output) {
  private val bytesInput: Input = new Input

  def apply(obj: AnyRef): Array[Byte] = {
    output.clear
    kryo.writeClassAndObject(output, obj)
    output.toBytes
  }

  def invert(b: Array[Byte]): Option[AnyRef] = {
    bytesInput.setBuffer(b)
    allCatch.opt(kryo.readClassAndObject(bytesInput))
  }

  def invert(s: InputStream): Option[AnyRef] = {
    // Input#setInputStream doesn't work, must use constructor
    val streamInput = new Input(s)
    val ret = allCatch.opt(kryo.readClassAndObject(streamInput))
    s.close()
    ret
  }
}
