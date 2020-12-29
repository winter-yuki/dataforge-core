package hep.dataforge.data

import hep.dataforge.meta.DFExperimental
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Lazy computation result with its dependencies to allowing to stat computing dependencies ahead of time
 */
public interface Goal<out T> {
    public val dependencies: Collection<Goal<*>>

    /**
     * Returns current running coroutine if the goal is started
     */
    public val result: Deferred<T>?

    /**
     * Get ongoing computation or start a new one.
     * Does not guarantee thread safety. In case of multi-thread access, could create orphan computations.
     */
    public fun startAsync(coroutineScope: CoroutineScope): Deferred<T>

    /**
     * Reset the computation
     */
    public fun reset()

    public companion object
}

public suspend fun <T> Goal<T>.await(): T = coroutineScope { startAsync(this).await() }

public val Goal<*>.isComplete: Boolean get() = result?.isCompleted ?: false

public open class StaticGoal<T>(public val value: T) : Goal<T> {
    override val dependencies: Collection<Goal<*>> get() = emptyList()
    override val result: Deferred<T> = CompletableDeferred(value)

    override fun startAsync(coroutineScope: CoroutineScope): Deferred<T> = result

    override fun reset() {
        //doNothing
    }
}

public open class ComputationGoal<T>(
    private val coroutineContext: CoroutineContext = EmptyCoroutineContext,
    override val dependencies: Collection<Goal<*>> = emptyList(),
    public val block: suspend CoroutineScope.() -> T,
) : Goal<T> {

    final override var result: Deferred<T>? = null
        private set

    /**
     * Get ongoing computation or start a new one.
     * Does not guarantee thread safety. In case of multi-thread access, could create orphan computations.
     */
    @DFExperimental
    override fun startAsync(coroutineScope: CoroutineScope): Deferred<T> {
        val startedDependencies = this.dependencies.map { goal ->
            goal.run { startAsync(coroutineScope) }
        }
        return result ?: coroutineScope.async(
            this.coroutineContext + CoroutineMonitor() + Dependencies(startedDependencies)
        ) {
            startedDependencies.forEach { deferred ->
                deferred.invokeOnCompletion { error ->
                    if (error != null) this.cancel(CancellationException("Dependency $deferred failed with error: ${error.message}"))
                }
            }
            block()
        }.also { result = it }
    }

    /**
     * Reset the computation
     */
    override fun reset() {
        result?.cancel()
        result = null
    }
}

/**
 * Create a one-to-one goal based on existing goal
 */
public fun <T, R> Goal<T>.map(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(T) -> R,
): Goal<R> = ComputationGoal(coroutineContext, listOf(this)) {
    block(await())
}

/**
 * Create a joining goal.
 */
public fun <T, R> Collection<Goal<T>>.reduce(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(Collection<T>) -> R,
): Goal<R> = ComputationGoal(coroutineContext, this) {
    block(map { run { it.await() } })
}

/**
 * A joining goal for a map
 * @param K type of the map key
 * @param T type of the input goal
 * @param R type of the result goal
 */
public fun <K, T, R> Map<K, Goal<T>>.reduce(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend CoroutineScope.(Map<K, T>) -> R,
): Goal<R> = ComputationGoal(coroutineContext, this.values) {
    block(mapValues { it.value.await() })
}

