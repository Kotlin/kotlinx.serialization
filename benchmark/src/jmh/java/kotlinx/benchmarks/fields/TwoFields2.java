package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class TwoFields2 {
    int i1;
    int i2;

    public TwoFields2(int mask, int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public static TwoFields2 deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        int var6 = 0;
        int var7 = 0;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);

        if (composite.readAll()) {
            var6 = composite.decodeIntElement(var2, 0);
            var7 = composite.decodeIntElement(var2, 1);
            composite.endStructure(var2);
            return new TwoFields2(Integer.MAX_VALUE, var6, var7);
        } else {
            while (true) {
                int var5 = 0;
                int var3 = composite.decodeElementIndex(var2);
                switch (var3) {
                    case 0:
                        var6 = composite.decodeIntElement(var2, 0);
                        var5 |= 1;
                    case 1:
                        var7 = composite.decodeIntElement(var2, 1);
                        var5 |= 2;
                    case -1:
                        composite.endStructure(var2);
                        return new TwoFields2(var5, var6, var7);
                    default:
                        throw new RuntimeException();
                }
            }
        }
    }

    public static void main(String[] args) {

    }
}
