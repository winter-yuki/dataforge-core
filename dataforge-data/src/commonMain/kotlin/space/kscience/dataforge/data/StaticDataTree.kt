package space.kscience.dataforge.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import space.kscience.dataforge.misc.DFExperimental
import space.kscience.dataforge.names.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@PublishedApi
internal class StaticDataTree<T : Any>(
    override val dataType: KType,
) : DataSetBuilder<T>, DataTree<T> {

    private val items: MutableMap<NameToken, DataTreeItem<T>> = HashMap()

    override suspend fun items(): Map<NameToken, DataTreeItem<T>> = items.filter { !it.key.body.startsWith("@") }

    override suspend fun remove(name: Name) {
        when (name.length) {
            0 -> error("Can't remove root tree node")
            1 -> items.remove(name.firstOrNull()!!)
            else -> (items[name.firstOrNull()!!].tree as? StaticDataTree<T>)?.remove(name.cutFirst())
        }
    }

    private fun getOrCreateNode(name: Name): StaticDataTree<T> = when (name.length) {
        0 -> this
        1 -> {
            val itemName = name.firstOrNull()!!
            (items[itemName].tree as? StaticDataTree<T>) ?: StaticDataTree<T>(dataType).also {
                items[itemName] = DataTreeItem.Node(it)
            }
        }
        else -> getOrCreateNode(name.cutLast()).getOrCreateNode(name.lastOrNull()!!.asName())
    }

    private suspend fun set(name: Name, item: DataTreeItem<T>?) {
        if (name.isEmpty()) error("Can't set top level tree node")
        if (item == null) {
            remove(name)
        } else {
            getOrCreateNode(name.cutLast()).items[name.lastOrNull()!!] = item
        }
    }

    override suspend fun emit(name: Name, data: Data<T>?) {
        set(name, data?.let { DataTreeItem.Leaf(it) })
    }

    override suspend fun emit(name: Name, dataSet: DataSet<T>) {
        if (dataSet is StaticDataTree) {
            set(name, DataTreeItem.Node(dataSet))
        } else {
            coroutineScope {
                dataSet.flowData().collect {
                    emit(name + it.name, it.data)
                }
            }
        }
    }
}

@Suppress("FunctionName")
public suspend fun <T : Any> DataTree(
    dataType: KType,
    block: suspend DataSetBuilder<T>.() -> Unit,
): DataTree<T> = StaticDataTree<T>(dataType).apply { block() }

@Suppress("FunctionName")
public suspend inline fun <reified T : Any> DataTree(
    noinline block: suspend DataSetBuilder<T>.() -> Unit,
): DataTree<T> = DataTree(typeOf<T>(), block)

@OptIn(DFExperimental::class)
public suspend fun <T : Any> DataSet<T>.seal(): DataTree<T> = DataTree(dataType){
    populate(this@seal)
}