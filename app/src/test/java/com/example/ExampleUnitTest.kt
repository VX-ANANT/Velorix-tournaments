
package com.example

import android.graphics.Path
import android.graphics.PathMeasure
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val path = Path()
        path.addRect(8f, 8f, 72f, 72f, Path.Direction.CW)
        val pm = PathMeasure(path, false)
        val pos = FloatArray(2)
        println("=== PATH MEASURE === len=" + pm.length)
        for (d in listOf(0f, 32f, 64f, 96f, 128f, 160f, 192f, 224f, 256f)) {
            pm.getPosTan(d, pos, null)
            println("d=" + d + ": (" + pos[0] + ", " + pos[1] + ")")
        }
    }
}
