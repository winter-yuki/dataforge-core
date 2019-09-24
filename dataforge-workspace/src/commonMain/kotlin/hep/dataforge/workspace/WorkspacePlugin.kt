package hep.dataforge.workspace

import hep.dataforge.context.AbstractPlugin
import hep.dataforge.context.toMap
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import kotlin.reflect.KClass

/**
 * An abstract plugin with some additional boilerplate to effectively work with workspace context
 */
abstract class WorkspacePlugin : AbstractPlugin() {
    private val _tasks = HashSet<Task<*>>()
    val tasks: Collection<Task<*>> get() = _tasks

    override fun provideTop(target: String): Map<Name, Any> {
        return when (target) {
            Task.TYPE -> tasks.toMap()
            else -> emptyMap()
        }
    }

    fun task(task: Task<*>){
        _tasks.add(task)
    }

    fun <T : Any> task(
        name: String,
        type: KClass<out T>,
        builder: TaskBuilder<T>.() -> Unit
    ): GenericTask<T> = TaskBuilder(name.toName(), type).apply(builder).build().also {
        _tasks.add(it)
    }

    inline fun <reified T : Any> task(
        name: String,
        noinline builder: TaskBuilder<T>.() -> Unit
    ) = task(name, T::class, builder)

//
////TODO add delegates to build gradle-like tasks
}