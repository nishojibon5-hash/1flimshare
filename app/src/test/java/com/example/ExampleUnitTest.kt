package com.example

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun calculatePasscodeHash() {
    val input = "flixbuzz2026"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    val hash = bytes.joinToString("") { "%02x".format(it) }
    assertEquals("The hash was: $hash", "751c728d3ba70e4f3f6f1289a41e519d23e92add9f9532e1514f55bb87baba36", hash)
  }
}
