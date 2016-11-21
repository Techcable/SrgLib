package net.techcable.srglib.format;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.io.LineProcessor;

import net.techcable.srglib.FieldData;
import net.techcable.srglib.JavaType;
import net.techcable.srglib.MethodData;
import net.techcable.srglib.MethodSignature;
import net.techcable.srglib.mappings.ImmutableMappings;
import net.techcable.srglib.mappings.Mappings;

/* package */ class CompactSrgMappingsFormat implements MappingsFormat {
    public static final CompactSrgMappingsFormat INSTANCE = new CompactSrgMappingsFormat();

    @Override
    public LineProcessor<Mappings> createLineProcessor() {
        return new SrgLineProcessor();
    }

    @Override
    public void write(Mappings mappings, Appendable output) throws IOException {
        try {
            mappings.forEachClass((original, renamed) -> {
                try {
                    output.append(original.getInternalName());
                    output.append(' ');
                    output.append(renamed.getInternalName());
                    output.append('\n');
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            mappings.forEachField((original, renamed) -> {
                try {
                    output.append(original.getDeclaringType().getInternalName());
                    output.append(' ');
                    output.append(original.getName());
                    output.append(' ');
                    output.append(renamed.getName());
                    output.append('\n');
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            mappings.forEachMethod((original, renamed) -> {
                try {
                    output.append(original.getDeclaringType().getInternalName());
                    output.append(' ');
                    output.append(original.getName());
                    output.append(' ');
                    output.append(original.getSignature().getDescriptor());
                    output.append(' ');
                    output.append(renamed.getName());
                    output.append('\n');
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /* package */ static class SrgLineProcessor implements LineProcessor<Mappings> {
        private final ImmutableBiMap.Builder<JavaType, JavaType> types = ImmutableBiMap.builder();
        // We have to queue the methods and fields, since the signatures of the renamed types need to be remapped
        private final Map<MethodData, String> methods = new LinkedHashMap<>();
        private final Map<FieldData, String> fields = new LinkedHashMap<>();

        @Override
        public boolean processLine(@Nonnull String line) throws IOException {
            parseLine(line);
            return true;
        }

        public void parseLine(@Nonnull String line) {
            line = line.trim(); // Strip whitespace
            if (line.startsWith("#") || line.isEmpty()) return;
            String[] args = line.split(" ");
            String originalName, newName;
            JavaType originalDeclaringType;
            switch (args.length) {
                case 2:
                    JavaType originalType = JavaType.fromInternalName(args[0]);
                    JavaType renamedType = JavaType.fromInternalName(args[1]);
                    types.put(originalType, renamedType);
                    break;
                case 3:
                    originalDeclaringType = JavaType.fromInternalName(args[0]);
                    originalName = args[1];
                    newName = args[2];
                    fields.put(FieldData.create(originalDeclaringType, originalName), newName);
                    break;
                case 4:
                    originalDeclaringType = JavaType.fromInternalName(args[0]);
                    originalName = args[1];
                    MethodSignature signature = MethodSignature.fromDescriptor(args[2]);
                    newName = args[3];
                    methods.put(MethodData.create(originalDeclaringType, originalName, signature), newName);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid line: " + line);
            }
        }

        @Override
        public Mappings getResult() {
            ImmutableBiMap<JavaType, JavaType> types = this.types.build();
            ImmutableBiMap.Builder<MethodData, MethodData> methods = ImmutableBiMap.builder();
            ImmutableBiMap.Builder<FieldData, FieldData> fields = ImmutableBiMap.builder();
            this.methods.forEach((originalData, newName) -> methods.put(originalData, originalData
                    .mapTypes(original -> types.getOrDefault(original, original))
                    .withName(newName)));
            this.fields.forEach((originalData, newName) -> fields.put(originalData, originalData
                    .mapTypes(original -> types.getOrDefault(original, original))
                    .withName(newName)));
            return ImmutableMappings.create(types, methods.build(), fields.build());
        }
    }
}
