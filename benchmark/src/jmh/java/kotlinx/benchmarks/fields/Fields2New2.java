package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields2New2 {
    int i1;
    int i2;

    public Fields2New2(int mask, int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public static Fields2New2 deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        if (composite.readAll()) {
            return readAll(var2, composite);
        } else {
            return byOne(var2, composite);
        }
    }

    private static Fields2New2 byOne(SerialDescriptor var2, CompositeDecoder composite) {
        int i1 = composite.decodeIntElement(var2, 1);
        int i2 = composite.decodeIntElement(var2, 2);
        composite.endStructure(var2);
        return new Fields2New2(Integer.MAX_VALUE, i1, i2);
    }

    private static Fields2New2 readAll(SerialDescriptor var2, CompositeDecoder composite) {

        int i1 = 0;
        int i2 = 0;
        int mask = 0;
        while (true) {
            int idx = composite.decodeElementIndex(var2);
            switch (idx) {
                case 0:
                    i1 = composite.decodeIntElement(var2, 0);
                    mask |= 2;
                    break;
                case 1:
                    i2 = composite.decodeIntElement(var2, 1);
                    mask |= 4;
                    break;
                case -1:
                    composite.endStructure(var2);
                    return new Fields2New2(mask, i1, i2);
                default:
                    throw new RuntimeException();
            }
        }
    }
}

