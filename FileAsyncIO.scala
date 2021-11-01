import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.{AsynchronousFileChannel, CompletionHandler}
import java.nio.file.Paths
import java.nio.file.StandardOpenOption._
import java.util.concurrent.atomic.AtomicLong
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try
import com.typesafe.scalalogging.StrictLogging


object FileAsyncIO {
  private var channel : Option[AsynchronousFileChannel] = None
  private var file : Option[String] = None 
  private var eof = new AtomicLong(0)
	
  def exists(file: String) = Paths.get(file).toFile.exists
  def closeSafely = try {channel.get.close()} catch {case e: IOException =>}	
  
  private def init(strFile : String) =  
	if (file.isEmpty|| !file.get.equals(strFile)) {
		if (channel.isEmpty)  
			channel = Some(AsynchronousFileChannel.open(Paths.get(strFile), WRITE))
		file = Some(strFile)
    }  
	  
	def appendText(file : String, s: String, charsetName: String = "UTF-8")(implicit ec: ExecutionContext) = {
		init(file)
		val buf = ByteBuffer.wrap(s.getBytes(charsetName))
		val currentEof = eof.getAndAdd(s.getBytes(charsetName).length)
		channel.get.write(buf, currentEof, channel.get, new WriteOp(buf,eof.get))
	}
	
	class WriteOp(pBuffer : ByteBuffer, pPosition : Long) extends  CompletionHandler[Integer, AsynchronousFileChannel] with StrictLogging { //(channel: AsynchronousFileChannel, p: Promise[Array[Byte]])
		private var position : Long = pPosition
		private var buffer : ByteBuffer = pBuffer
		def completed(res: Integer, channel: AsynchronousFileChannel): Unit = {
		if ( buffer.remaining > 0 ) {	
			position += res.toLong //position = buffer.position
			channel.write( buffer, position, channel, this)
				}
		}
		def failed(t: Throwable, channel: AsynchronousFileChannel): Unit = {
				logger.error("[ERROR] "+t)
				closeSafely
		}
	}
}