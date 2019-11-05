package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields1New {
    int i1;

    public Fields1New(int mask, int i1) {
        this.i1 = i1;
    }

    public static Fields1New deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        int i1 = 0;
        if (composite.readAll()) {
            i1 = composite.decodeIntElement(var2, 1);
            composite.endStructure(var2);
            return new Fields1New(Integer.MAX_VALUE, i1);
        } else {
            int mask = 0;
            while (true) {
                int idx = composite.decodeElementIndex(var2);
                switch (idx) {
                    case 0:
                        i1 = composite.decodeIntElement(var2, 0);
                        mask |= 0;
                        break;
                    case -1:
                        composite.endStructure(var2);
                        return new Fields1New(mask, i1);
                    default:
                        throw new RuntimeException();
                }
            }
        }
    }
}

