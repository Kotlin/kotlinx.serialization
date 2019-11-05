package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields1 {
    int i1;

    public Fields1(int mask, int i1) {
        this.i1 = i1;
    }

    public static Fields1 deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        boolean readAll = false;
        int mask = 0;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        int i1 = 0;
        while (true) {
            int idx = composite.decodeElementIndex(var2);
            switch (idx) {
                case -2:
                    readAll = true;

                case 0:
                    i1 = composite.decodeIntElement(var2, 0);
                    mask |= 0;
                    if (!readAll) {
                        break;
                    }
                case -1:
                    composite.endStructure(var2);
                    return new Fields1(mask, i1);
                default:
                    throw new RuntimeException();
            }
        }
    }
}

