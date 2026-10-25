package dev.odoorunner;

import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** Poll only bytes appended since launch; handle truncation and normal log rotation. */
final class LogFileFollower {
    private final Path path;
    private final RunnerLogs logs;
    private long position;
    private Object fileKey;
    private byte[] carry=new byte[0];
    private String lastError="";
    LogFileFollower(Path path,RunnerLogs logs){this.path=path;this.logs=logs;try{var attributes=Files.readAttributes(path,BasicFileAttributes.class);position=attributes.size();fileKey=attributes.fileKey();}catch(Exception ignored){}}
    void poll(){try{
        if(!Files.exists(path))return;var attributes=Files.readAttributes(path,BasicFileAttributes.class);if(attributes.size()<position||(fileKey!=null&&!java.util.Objects.equals(fileKey,attributes.fileKey()))){position=0;carry=new byte[0];logs.message("[Log file rotated/truncated]");}fileKey=attributes.fileKey();if(attributes.size()==position){lastError="";return;}
        if(attributes.size()-position>262144){position=attributes.size()-262144;carry=new byte[0];logs.message("WARNING [Log reader skipped older bytes during a large output burst]");}
        try(FileChannel file=FileChannel.open(path,StandardOpenOption.READ)){file.position(position);ByteBuffer buffer=ByteBuffer.allocate(65536);int read=file.read(buffer);if(read>0){position+=read;buffer.flip();ByteBuffer joined=ByteBuffer.allocate(carry.length+read);joined.put(carry).put(buffer).flip();CharsetDecoder decoder=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);CharBuffer decoded=CharBuffer.allocate(joined.remaining()+1);decoder.decode(joined,decoded,false);carry=new byte[joined.remaining()];joined.get(carry);decoded.flip();logs.accept("logfile",decoded.toString());}}
        lastError="";
    }catch(Exception ex){String message=ex.getClass().getSimpleName();if(!lastError.equals(message)){logs.message("WARNING [Cannot read configured log file: "+message+"]");lastError=message;}}}
}
