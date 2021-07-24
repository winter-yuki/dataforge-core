package space.kscience.dataforge.meta

import space.kscience.dataforge.names.Name
import space.kscience.dataforge.names.NameToken
import space.kscience.dataforge.names.asName
import space.kscience.dataforge.names.startsWith
import space.kscience.dataforge.values.Value
import kotlin.jvm.Synchronized
import kotlin.reflect.KProperty1


internal data class MetaListener(
    val owner: Any? = null,
    val callback: Meta.(name: Name) -> Unit,
)

/**
 * An item provider that could be observed and mutated
 */
public interface ObservableMeta : Meta {
    /**
     * Add change listener to this meta. Owner is declared to be able to remove listeners later. Listener without owner could not be removed
     */
    public fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit)

    /**
     * Remove all listeners belonging to given owner
     */
    public fun removeListener(owner: Any?)
}

/**
 * A [Meta] which is both observable and mutable
 */
public interface ObservableMutableMeta : ObservableMeta, MutableTypedMeta<ObservableMutableMeta>

private class ObservableMetaWrapper<M : MutableTypedMeta<M>>(
    val origin: M,
) : ObservableMutableMeta,  Meta by origin {

    private val listeners = HashSet<MetaListener>()

    private fun changed(name: Name) {
        listeners.forEach { it.callback(this, name) }
    }

    @Synchronized
    override fun onChange(owner: Any?, callback: Meta.(name: Name) -> Unit) {
        listeners.add(MetaListener(owner, callback))
    }

    @Synchronized
    override fun removeListener(owner: Any?) {
        listeners.removeAll { it.owner === owner }
    }

    override val items: Map<NameToken, ObservableMetaWrapper<M>>
        get() = origin.items.mapValues { ObservableMetaWrapper(it.value) }

    override var value: Value?
        get() = origin.value
        set(value) {
            origin.value = value
            changed(Name.EMPTY)
        }

    override fun attach(name: Name, node: ObservableMutableMeta) {
        origin.attach(name, node.origin)
        changed(name)
    }

    override fun getOrCreate(name: Name): ObservableMutableMeta =
        get(name) ?: ObservableMetaWrapper(origin.getOrCreate(name))


    override fun removeNode(name: Name) {
        origin.removeNode(name)
        changed(name)
    }

    override fun set(name: Name, meta: Meta) {
        val oldMeta = get(name)
        origin[name] = meta
        if (oldMeta != meta) {
            changed(name)
        }
    }

    override fun toMeta(): Meta {
        return origin.toMeta()
    }
}

public fun <M : MutableTypedMeta<M>> MutableTypedMeta<M>.asObservable(): ObservableMeta =
    (this as? ObservableMeta) ?: ObservableMetaWrapper(self)


/**
 * Use the value of the property in a [callBack].
 * The callback is called once immediately after subscription to pass the initial value.
 *
 * Optional [owner] property is used for
 */
public fun <O : ObservableMeta, T> O.useProperty(
    property: KProperty1<O, T>,
    owner: Any? = null,
    callBack: O.(T) -> Unit,
) {
    //Pass initial value.
    callBack(property.get(this))
    onChange(owner) { name ->
        if (name.startsWith(property.name.asName())) {
            callBack(property.get(this@useProperty))
        }
    }
}