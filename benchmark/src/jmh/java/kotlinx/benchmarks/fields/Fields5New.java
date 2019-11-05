package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields5New {
    int i1;
    int i2;
    int i3;
    int i4;
    int i5;

    public Fields5New(int mask, int i1, int i2, int i3, int i4, int i5) {
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.i4 = i4;
        this.i5 = i5;
    }

    public static Fields5New deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        if (composite.readAll()) {
            i1 = composite.decodeIntElement(var2, 1);
            i2 = composite.decodeIntElement(var2, 2);
            i3 = composite.decodeIntElement(var2, 3);
            i4 = composite.decodeIntElement(var2, 4);
            i5 = composite.decodeIntElement(var2, 5);
            composite.endStructure(var2);
            return new Fields5New(Integer.MAX_VALUE, i1, i2, i3, i4, i5);
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
                    case 2:
                        i3 = composite.decodeIntElement(var2, 2);
                        mask |= 4;
                        break;
                    case 3:
                        i4 = composite.decodeIntElement(var2, 3);
                        mask |= 8;
                        break;
                    case 4:
                        i5 = composite.decodeIntElement(var2, 4);
                        mask |= 16;
                        break;
                    case -1:
                        composite.endStructure(var2);
                        return new Fields5New(mask, i1, i2, i3, i4, i5);
                    default:
                        throw new RuntimeException();
                }
            }
        }
    }
}

