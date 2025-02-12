package space.kscience.dataforge.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import space.kscience.dataforge.data.Data.Companion.TYPE_OF_NOTHING
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.set
import space.kscience.dataforge.names.*
import kotlin.reflect.KType

public interface DataSet<out T : Any> {

    /**
     * The minimal common ancestor to all data in the node
     */
    public val dataType: KType

    /**
     * Traverse this provider or its child. The order is not guaranteed.
     */
    public fun flowData(): Flow<NamedData<T>>

    /**
     * Get data with given name.
     */
    public suspend fun getData(name: Name): Data<T>?

    /**
     * Get a snapshot of names of top level children of given node. Empty if node does not exist or is a leaf.
     */
    public suspend fun listTop(prefix: Name = Name.EMPTY): List<Name> =
        flowData().map { it.name }.filter { it.startsWith(prefix) && (it.length == prefix.length + 1) }.toList()
    // By default, traverses the whole tree. Could be optimized in descendants

    public companion object {
        public val META_KEY: Name = "@meta".asName()

        /**
         * An empty [DataSet] that suits all types
         */
        public val EMPTY: DataSet<Nothing> = object : DataSet<Nothing> {
            override val dataType: KType = TYPE_OF_NOTHING

            //private val nothing: Nothing get() = error("this is nothing")

            override fun flowData(): Flow<NamedData<Nothing>> = emptyFlow()

            override suspend fun getData(name: Name): Data<Nothing>? = null
        }
    }
}

public interface ActiveDataSet<T : Any> : DataSet<T> {
    /**
     * A flow of updated item names. Updates are propagated in a form of [Flow] of names of updated nodes.
     * Those can include new data items and replacement of existing ones. The replaced items could update existing data content
     * and replace it completely, so they should be pulled again.
     *
     */
    public val updates: Flow<Name>
}

public val <T : Any> DataSet<T>.updates: Flow<Name> get() = if (this is ActiveDataSet) updates else emptyFlow()

/**
 * Flow all data nodes with names starting with [branchName]
 */
public fun <T : Any> DataSet<T>.flowChildren(branchName: Name): Flow<NamedData<T>> = this@flowChildren.flowData().filter {
    it.name.startsWith(branchName)
}

/**
 * Start computation for all goals in data node and return a job for the whole node
 */
public fun <T : Any> DataSet<T>.startAll(coroutineScope: CoroutineScope): Job = coroutineScope.launch {
    flowData().map {
        it.launch(this@launch)
    }.toList().joinAll()
}

public suspend fun <T : Any> DataSet<T>.join(): Unit = coroutineScope { startAll(this).join() }

public suspend fun DataSet<*>.toMeta(): Meta = Meta {
    flowData().collect {
        if (it.name.endsWith(DataSet.META_KEY)) {
            set(it.name, it.meta)
        } else {
            it.name put {
                "type" put it.type.toString()
                "meta" put it.meta
            }
        }
    }
}

public val <T : Any> DataSet<T>.updatesWithData: Flow<NamedData<T>> get() = updates.mapNotNull { getData(it)?.named(it) }