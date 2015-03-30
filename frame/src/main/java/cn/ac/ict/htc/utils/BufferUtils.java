package cn.ac.ict.htc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;

public class BufferUtils {
	static public ByteBuffer readFileToByteBuffer(File file, long offset,
			long readSize, boolean isDirect) throws IOException {
		final long fileSize = Math.max(file.length(), 0l);
		offset = Math.max(offset, 0l);
		offset = Math.min(offset, fileSize);

		if (0 >= readSize)
		{
			readSize = fileSize - offset;
		}
		readSize = Math.min(readSize, fileSize - offset);
		readSize = Math.min(readSize, Integer.MAX_VALUE);
		
		if (0 < readSize && file.isFile()) {
			allocateBuffer((int) readSize, isDirect);
			
			final FileInputStream inFileStr = FileUtils.openInputStream(file);
			IOUtils.skip(inFileStr, offset);
			final BoundedInputStream inStr = new BoundedInputStream(inFileStr, readSize);

			final ByteBuffer buffer = allocateBuffer((int) readSize, isDirect);
			final OutputStream outStr = new ByteBufferBackedOutputStream(buffer);

			IOUtils.copy(inStr, outStr);
			return buffer;
		}
		return null;
	}

	static public int readFileToByteBuffer(File file, long offset,
			long readSize, ByteBuffer buffer) throws IOException {
		final long fileSize = Math.max(file.length(), 0l);
		offset  = Math.max(offset, 0l);
		offset = Math.min(offset, fileSize);
		
		if (0 >= readSize)
		{
			readSize = fileSize - offset;
		}
		readSize = Math.min(readSize, fileSize - offset);
		readSize = Math.min(readSize, buffer.remaining());
		
		int positionBefore = buffer.position();
		if (0 < readSize && file.isFile()) {
			final FileInputStream inFileStr = FileUtils.openInputStream(file);
			IOUtils.skip(inFileStr, offset);
			final BoundedInputStream inStr = new BoundedInputStream(inFileStr, readSize);
			
			final OutputStream outStr = new ByteBufferBackedOutputStream(buffer);

			IOUtils.copy(inStr, outStr);
		}
	
		return buffer.position() - positionBefore;
	}

	
	public static ByteBuffer allocateBuffer(int size, boolean isDirect) {
		final ByteBuffer buffer;
		if (isDirect)
		{
			buffer = ByteBuffer.allocateDirect(size);
		}
		else
		{
			buffer = ByteBuffer.allocate(size);
		}
		return buffer;
	}
	
	public static ByteBuffer writeByteBufferToFile(ByteBuffer buffer, File file) throws IOException
	{
		return writeByteBufferToFile(buffer, file, false);
	}

	public static ByteBuffer writeByteBufferToFile(ByteBuffer buffer, File file, boolean append) throws IOException
	{
		if (file.exists() && file.isDirectory())
		{
			throw new FileNotFoundException("A directory with same name does exiest.");
		}
		
		FileUtils.touch(file);
		final ByteBufferBackedInputStream inStr = new ByteBufferBackedInputStream(
				buffer);
		final FileOutputStream outStr = FileUtils.openOutputStream(file);

		IOUtils.copy(inStr, outStr);
		
		return buffer;
	}

	public static ByteBuffer allocateNativeOrderDirectBuffer(int size){
		ByteBuffer buf = allocateBuffer(size, true);
		buf.order(ByteOrder.nativeOrder());
		return buf;
	}
}
