package space.kscience.dataforge.meta

import space.kscience.dataforge.values.Value
import space.kscience.dataforge.values.asValue
import kotlin.test.Test
import kotlin.test.assertEquals


class MetaBuilderTest {
    @Test
    fun testBuilder() {
        val meta = Meta {
            "a" put 22
            "b" put Value.of(listOf(1, 2, 3))
            this["c"] = "myValue".asValue()
            "node" put {
                "e" put 12.2
                "childNode" put {
                    "f" put true
                }
            }
        }
        assertEquals(12.2, meta["node.e"]?.double)
        assertEquals(true, meta["node.childNode.f"]?.boolean)
    }

    @Test
    fun testSNS(){
        val meta = Meta {
            repeat(10){
                "b.a[$it]" put it
            }
        }.seal()
        assertEquals(10, meta.valueSequence().count())

        val nodes = meta.getIndexed("b.a")

        assertEquals(3, nodes["3"]?.int)
    }

}