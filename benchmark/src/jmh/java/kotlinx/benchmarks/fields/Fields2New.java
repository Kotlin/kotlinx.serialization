package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields2New {
    int i1;
    int i2;

    public Fields2New(int mask, int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    public static Fields2New deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        int i1 = 0;
        int i2 = 0;
        if (composite.readAll()) {
            i1 = composite.decodeIntElement(var2, 1);
            i2 = composite.decodeIntElement(var2, 2);
            composite.endStructure(var2);
            return new Fields2New(Integer.MAX_VALUE, i1, i2);
        } else {
            int mask = 0;
            while (true) {
                int idx = composite.decodeElementIndex(var2);
                switch (idx) {
                    case 0:
                        i1 = composite.decodeIntElement(var2, 0);
                        mask |= 0;
                        break;
                    case 1:
                        i2 = composite.decodeIntElement(var2, 1);
                        mask |= 2;
                        break;
                    case -1:
                        composite.endStructure(var2);
                        return new Fields2New(mask, i1, i2);
                    default:
                        throw new RuntimeException();
                }
            }
        }
    }
}

