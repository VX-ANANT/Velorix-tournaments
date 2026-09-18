package com.example
import com.entrig.sdk.Entrig
import org.junit.Test
class EntrigMethodsTest {
    @Test
    fun printMethods() {
        println("=== ENTRIG METHODS ===")
        Entrig::class.java.declaredMethods.forEach { println(it) }
        println("======================")
    }
}
