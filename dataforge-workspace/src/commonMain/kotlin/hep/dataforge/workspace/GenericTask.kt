package hep.dataforge.workspace

import hep.dataforge.context.logger
import hep.dataforge.data.DataSet
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.NodeDescriptor
import hep.dataforge.meta.get
import hep.dataforge.meta.node
import hep.dataforge.names.Name
import kotlin.reflect.KClass

//data class TaskEnv(val workspace: Workspace, val model: TaskModel)


public class GenericTask<R : Any>(
    override val name: Name,
    override val type: KClass<out R>,
    override val descriptor: NodeDescriptor,
    private val modelTransform: TaskModelBuilder.(Meta) -> Unit,
    private val dataTransform: Workspace.() -> suspend TaskModel.(DataSet<Any>) -> DataSet<R>
) : Task<R> {

    override suspend fun run(workspace: Workspace, model: TaskModel): DataSet<R> {
        //validate model
        validate(model)

        // gather data
        val input = model.buildInput(workspace)// gather(workspace, model)

        //execute
        workspace.logger.info{"Starting task '$name' on ${model.target} with meta: \n${model.meta}"}
        val output = dataTransform(workspace).invoke(model, input)

        //handle result
        //output.handle(model.context.dispatcher) { this.handle(it) }

        return output
    }

    /**
     * Build new TaskModel and apply specific model transformation for this
     * task. By default model uses the meta node with the same node as the name of the task.
     *
     * @param workspace
     * @param taskMeta
     * @return
     */
    override fun build(workspace: Workspace, taskMeta: Meta): TaskModel {
        val taskMeta = taskMeta[name]?.node ?: taskMeta
        val builder = TaskModelBuilder(name, taskMeta)
        builder.modelTransform(taskMeta)
        return builder.build()
    }
    //TODO add validation
}