package net.techcable.srglib.format;

import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import com.google.common.io.LineReader;

import net.techcable.srglib.mappings.Mappings;
import net.techcable.srglib.utils.Exceptions;

import static net.techcable.srglib.utils.Exceptions.*;

/**
 * A format for serializing mappings to and from text.
 */
public interface MappingsFormat {
    MappingsFormat SEARGE_FORMAT = SrgMappingsFormat.INSTANCE;
    MappingsFormat COMPACT_SEARGE_FORMAT = CompactSrgMappingsFormat.INSTANCE;

    default Mappings parse(Readable readable) throws IOException {
        LineReader lineReader = new LineReader(readable);
        LineProcessor<Mappings> lineProcessor = createLineProcessor();
        String line;
        while ((line = lineReader.readLine()) != null) {
            if (!lineProcessor.processLine(line)) {
                break;
            }
        }
        return lineProcessor.getResult();
    }

    default Mappings parseLines(String... lines) {
        return parseLines(Arrays.asList(lines));
    }

    default Mappings parseLines(Iterable<String> lines) {
        return parseLines(lines.iterator());
    }

    default Mappings parseLines(Iterator<String> lines) {
        LineProcessor<Mappings> lineProcessor = createLineProcessor();
        lines.forEachRemaining(Exceptions.sneakyThrowing(lineProcessor::processLine));
        return lineProcessor.getResult();
    }

    LineProcessor<Mappings> createLineProcessor();

    void write(Mappings mappings, Appendable output) throws IOException;

    default List<String> toLines(Mappings mappings) {
        CharArrayWriter result = new CharArrayWriter();
        return sneakyThrowing(() -> {
            this.write(mappings, result);
            return CharStreams.readLines(new CharArrayReader(result.toCharArray()));
        }).get();
    }
}
