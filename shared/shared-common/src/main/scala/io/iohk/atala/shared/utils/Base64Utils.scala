package io.iohk.atala.shared.utils

import java.nio.charset.StandardCharsets
import java.util.Base64

object Base64Utils {
  def encodeURL(bytes: Array[Byte]): String = {
    Base64.getUrlEncoder.encodeToString(bytes)
  }

  def decodeUrlToString(encodedStr: String): String = {
    new String(Base64.getUrlDecoder.decode(encodedStr), StandardCharsets.UTF_8)
  }

  def decodeURL(string: String): Array[Byte] = {
    Base64.getUrlDecoder.decode(string)
  }
}
