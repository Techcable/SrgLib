package net.techcable.srglib;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.techcable.srglib.format.MappingsFormat;
import net.techcable.srglib.mappings.Mappings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class MappingsFormatTest {
    private static final ImmutableList<String> TEST_LINES = ImmutableList.of(
            "CL: org/spigotmc/XRay net/techcable/xray/XRay",
            "CL: org/spigotmc/XRay$Manager net/techcable/xray/XRayManager",
            "CL: org/spigotmc/XRay$Injector net/techcable/xray/injector/Injector",
            "CL: org/spigotmc/XRay$Injector$Manager net/techcable/xray/injector/InjectorManager",
            "CL: obfs net/techcable/minecraft/NoHax",
            "CL: obf4 net/techcable/minecraft/Player",
            "FD: obf4/a net/techcable/minecraft/Player/dead",
            "FD: obf4/b net/techcable/minecraft/Player/blood",
            "FD: obf4/c net/techcable/minecraft/Player/health",
            "FD: obf4/d net/techcable/minecraft/Player/speed",
            "FD: org/spigotmc/XRay$Injector$Manager/taco net/techcable/xray/injector/InjectorManager/seriousVariableName",
            "MD: obfs/a (Lobf4;ID)Z net/techcable/minecraft/NoHax/isHacking (Lnet/techcable/minecraft/Player;ID)Z",
            "MD: org/spigotmc/XRay/deobfuscate ([BLjava/util/Set;)I net/techcable/xray/XRay/doAFunkyDance ([BLjava/util/Set;)I",
            "MD: org/spigotmc/XRay$Manager/aquire ()Lorg/spigotmc/XRay; net/techcable/xray/XRayManager/get ()Lnet/techcable/xray/XRay;"
    );
    private static final ImmutableList<String> COMPACT_TEST_LINES = ImmutableList.of(
            "org/spigotmc/XRay net/techcable/xray/XRay",
            "org/spigotmc/XRay$Manager net/techcable/xray/XRayManager",
            "org/spigotmc/XRay$Injector net/techcable/xray/injector/Injector",
            "org/spigotmc/XRay$Injector$Manager net/techcable/xray/injector/InjectorManager",
            "obfs net/techcable/minecraft/NoHax",
            "obf4 net/techcable/minecraft/Player",
            "obf4 a dead",
            "obf4 b blood",
            "obf4 c health",
            "obf4 d speed",
            "org/spigotmc/XRay$Injector$Manager taco seriousVariableName",
            "obfs a (Lobf4;ID)Z isHacking",
            "org/spigotmc/XRay deobfuscate ([BLjava/util/Set;)I doAFunkyDance",
            "org/spigotmc/XRay$Manager aquire ()Lorg/spigotmc/XRay; get"
    );
    @Parameterized.Parameters
    public static Object[][] mappingFormats() {
        return new Object[][] {
                new Object[] {  MappingsFormat.SEARGE_FORMAT, TEST_LINES },
                new Object[] {  MappingsFormat.COMPACT_SEARGE_FORMAT, COMPACT_TEST_LINES }
        };
    }
    private final MappingsFormat mappingsFormat;
    private final ImmutableList<String> testLines;
    public MappingsFormatTest(MappingsFormat mappingsFormat, ImmutableList<String> testLines) {
        this.mappingsFormat = mappingsFormat;
        this.testLines = testLines;
    }

    @Test
    public void testParse() {
        Mappings result = mappingsFormat.parseLines(testLines);
        assertEquals("net.techcable.xray.XRay", result.getNewClass("org.spigotmc.XRay").getName());
        assertEquals("net.techcable.minecraft.Player", result.getNewClass("obf4").getName());
        assertEquals(
                MethodData.create(
                        JavaType.fromName("net.techcable.minecraft.NoHax"),
                        "isHacking",
                        ImmutableList.of(
                                JavaType.fromName("net.techcable.minecraft.Player"),
                                PrimitiveType.INT,
                                PrimitiveType.DOUBLE
                        ),
                        PrimitiveType.BOOLEAN
                ),
                result.getNewMethod(MethodData.create(
                        JavaType.fromName("obfs"),
                        "a",
                        ImmutableList.of(
                                JavaType.fromName("obf4"),
                                PrimitiveType.INT,
                                PrimitiveType.DOUBLE
                        ),
                        PrimitiveType.BOOLEAN
                ))
        );
        assertEquals(
                FieldData.create(JavaType.fromName("net.techcable.minecraft.Player"), "dead"),
                result.getNewField(FieldData.create(JavaType.fromName("obf4"), "a"))
        );
    }

    @Test
    public void testSerialize() {
        Mappings expected = mappingsFormat.parseLines(testLines);
        List<String> serialized = mappingsFormat.toLines(expected);
        Mappings actual = mappingsFormat.parseLines(serialized);
        assertEquals(expected, actual);
    }
}
