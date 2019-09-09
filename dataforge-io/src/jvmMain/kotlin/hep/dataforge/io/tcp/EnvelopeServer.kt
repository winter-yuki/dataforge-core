package hep.dataforge.io.tcp

import hep.dataforge.context.Context
import hep.dataforge.context.ContextAware
import hep.dataforge.io.EnvelopeFormat
import hep.dataforge.io.Responder
import hep.dataforge.io.TaggedEnvelopeFormat
import hep.dataforge.io.type
import kotlinx.coroutines.*
import kotlinx.io.streams.writePacket
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class EnvelopeServer(
    override val context: Context,
    val port: Int,
    val responder: Responder,
    val scope: CoroutineScope,
    val format: EnvelopeFormat = TaggedEnvelopeFormat
) : ContextAware {

    private var job: Job? = null

    fun start() {
        if (job == null) {
            logger.info { "Starting envelope server on port $port" }
            val job = scope.launch(Dispatchers.IO) {
                val serverSocket = ServerSocket(port)
                //TODO add handshake and format negotiation
                while (isActive && !serverSocket.isClosed) {
                    val socket = serverSocket.accept()
                    logger.info { "Accepted connection from ${socket.remoteSocketAddress}" }
                    readSocket(socket)
                }
            }
        }
    }

    fun stop() {
        logger.info { "Stopping envelope server on port $port" }
        job?.cancel()
        job = null
    }

//    private fun CoroutineScope.readSocket(socket: Socket) {
//        launch(Dispatchers.IO) {
//            val input = socket.getInputStream().asInput()
//            val output = socket.getOutputStream().asOutput()
//            format.run {
//                while (isActive && socket.isConnected) {
//                    val request = input.readThis()
//                    logger.debug { "Accepted request with type ${request.type} from ${socket.remoteSocketAddress}" }
//                    if (request.type == SHUTDOWN_ENVELOPE_TYPE) {
//                        //Echo shutdown command
//                        logger.info { "Accepted graceful shutdown signal from ${socket.inetAddress}" }
//                        socket.close()
//                        cancel("Graceful connection shutdown requested by client")
//                    }
//                    val response = responder.respond(request)
//                    output.writeThis(response)
//                }
//            }
//        }
//    }

    private fun readSocket(socket: Socket) {
        thread {
            val input = socket.getInputStream().asInput()
            val outputStream = socket.getOutputStream()
            format.run {
                while (socket.isConnected) {
                    val request = input.readThis()
                    logger.debug { "Accepted request with type ${request.type} from ${socket.remoteSocketAddress}" }
                    if (request.type == SHUTDOWN_ENVELOPE_TYPE) {
                        //Echo shutdown command
                        logger.info { "Accepted graceful shutdown signal from ${socket.inetAddress}" }
                        socket.close()
                        return@thread
//                        cancel("Graceful connection shutdown requested by client")
                    }
                    runBlocking {
                        val response = responder.respond(request)
                        outputStream.writePacket {
                            writeThis(response)
                        }
                        logger.debug { "Sent response with type ${response.type} to ${socket.remoteSocketAddress}" }
                    }
                }
            }
        }
    }

    companion object {
        const val SHUTDOWN_ENVELOPE_TYPE = "@shutdown"
    }
}