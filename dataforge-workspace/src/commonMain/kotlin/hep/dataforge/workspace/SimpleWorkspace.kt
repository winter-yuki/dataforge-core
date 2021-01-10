package hep.dataforge.workspace

import hep.dataforge.context.Context
import hep.dataforge.context.gather
import hep.dataforge.context.toMap
import hep.dataforge.data.DataTree
import hep.dataforge.meta.Meta
import hep.dataforge.names.Name


/**
 * A simple workspace without caching
 */
public class SimpleWorkspace(
    override val context: Context,
    override val data: DataTree<Any>,
    override val targets: Map<String, Meta>,
    tasks: Collection<Task<Any>>
) : Workspace {

    override val tasks: Map<Name, Task<*>> by lazy {
        context.gather<Task<*>>(Task.TYPE) + tasks.toMap()
    }

    public companion object {

    }
}