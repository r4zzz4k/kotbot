package io.heapy.kotbot.bot.rule

import org.junit.jupiter.api.*
import java.io.InputStream

class ImagePolicyRuleTests {
    @Test
    fun `dump image samples`() {
        fun dumpHistogram(title: String, histogram: FloatArray) {
            println("$title: ${histogram.joinToString { "%4.2f".format(it) }}")
            repeat(16) { row ->
                val valueRange = 1 - (row + 1) / 16f .. 1 - row / 16f
                histogram.forEach { col ->
                    print(if(col in valueRange) '#' else ' ')
                }
                println()
            }
        }

        fun dumpImage(name: String) {
            val img = getResourceAsStream("bannedimages/$name")
            val (red, green, blue) = getImageHistogram(img)
            println("================================================================")
            println("Dumping $name:")
            dumpHistogram("Red", red)
            dumpHistogram("Green", green)
            dumpHistogram("Blue", blue)
        }

        dumpImage("set01_01.jpg")
        dumpImage("set01_02.jpg")
        dumpImage("set01_03.jpg")

        Assertions.assertEquals(true, true)
    }

    private fun getResourceAsStream(name: String): InputStream =
        ImagePolicyRuleTests::class.java.classLoader.getResourceAsStream(name)!!
}
