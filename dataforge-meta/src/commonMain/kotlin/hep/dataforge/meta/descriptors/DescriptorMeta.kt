package hep.dataforge.meta.descriptors

import hep.dataforge.meta.Laminate
import hep.dataforge.meta.MetaBase
import hep.dataforge.meta.MetaItem
import hep.dataforge.names.NameToken
import hep.dataforge.values.Null

/**
 * A [Meta] that wraps a descriptor node
 */
class DescriptorMeta(val descriptor: NodeDescriptor) : MetaBase() {
    override val items: Map<NameToken, MetaItem<*>>
        get() = descriptor.items.entries.associate { entry ->
            NameToken(entry.key) to entry.value.defaultItem()
        }
}

fun NodeDescriptor.buildDefaultMeta() = Laminate(default, DescriptorMeta(this))

fun NodeDescriptor.defaultItem(): MetaItem.NodeItem<*> =
    MetaItem.NodeItem(buildDefaultMeta())

fun ValueDescriptor.defaultItem(): MetaItem.ValueItem = MetaItem.ValueItem(default ?: Null)

/**
 * Build a default [MetaItem] from descriptor.
 */
fun ItemDescriptor.defaultItem(): MetaItem<*> {
    return when (this) {
        is ValueDescriptor -> defaultItem()
        is NodeDescriptor -> defaultItem()
    }
}