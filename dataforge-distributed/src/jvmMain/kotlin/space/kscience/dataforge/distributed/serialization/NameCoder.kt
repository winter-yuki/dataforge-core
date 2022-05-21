package space.kscience.dataforge.distributed.serialization

import io.lambdarpc.coding.Coder
import io.lambdarpc.coding.CodingContext
import io.lambdarpc.transport.grpc.Entity
import io.lambdarpc.transport.serialization.Entity
import io.lambdarpc.transport.serialization.RawData
import kotlinx.serialization.json.Json
import space.kscience.dataforge.names.Name
import java.nio.charset.Charset

internal object NameCoder : Coder<Name> {
    override suspend fun decode(entity: Entity, context: CodingContext): Name {
        val string = entity.data.toString(Charset.defaultCharset())
        return Json.decodeFromString(Name.serializer(), string)
    }

    override suspend fun encode(value: Name, context: CodingContext): Entity {
        val string = Json.encodeToString(Name.serializer(), value)
        return Entity(RawData.copyFrom(string, Charset.defaultCharset()))
    }
}
