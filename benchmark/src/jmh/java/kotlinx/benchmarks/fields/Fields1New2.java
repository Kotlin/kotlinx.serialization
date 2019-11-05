package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields1New2 {
    int i1;

    public Fields1New2(int mask, int i1) {
        this.i1 = i1;
    }

    public static Fields1New2 deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        if (composite.readAll()) {
            return readAll(var2, composite);
        } else {
            return byOne(var2, composite);
        }
    }

    private static Fields1New2 byOne(SerialDescriptor var2, CompositeDecoder composite) {
        int i1 = composite.decodeIntElement(var2, 1);
        composite.endStructure(var2);
        return new Fields1New2(Integer.MAX_VALUE, i1);
    }

    private static Fields1New2 readAll(SerialDescriptor var2, CompositeDecoder composite) {

        int i1 = 0;
        int mask = 0;
        while (true) {
            int idx = composite.decodeElementIndex(var2);
            switch (idx) {
                case 0:
                    i1 = composite.decodeIntElement(var2, 0);
                    mask |= 2;
                    break;
                case -1:
                    composite.endStructure(var2);
                    return new Fields1New2(mask, i1);
                default:
                    throw new RuntimeException();
            }
        }
    }
}

