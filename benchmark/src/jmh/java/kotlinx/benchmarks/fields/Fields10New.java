package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields10New {
    int i1;
    int i2;
    int i3;
    int i4;
    int i5;
    int i6;
    int i7;
    int i8;
    int i9;
    int i10;

    public Fields10New(int mask, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        this.i1 = i1;
        this.i2 = i2;
        this.i3 = i3;
        this.i4 = i4;
        this.i5 = i5;
        this.i6 = i6;
        this.i7 = i7;
        this.i8 = i8;
        this.i9 = i9;
        this.i10 = i10;
    }

    public static Fields10New deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        CompositeDecoder composite = decoder.beginStructure(var2, new KSerializer[0]);
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        int i9 = 0;
        int i10 = 0;
        if (composite.readAll()) {
            i1 = composite.decodeIntElement(var2, 1);
            i2 = composite.decodeIntElement(var2, 2);
            i3 = composite.decodeIntElement(var2, 3);
            i4 = composite.decodeIntElement(var2, 4);
            i5 = composite.decodeIntElement(var2, 5);
            i6 = composite.decodeIntElement(var2, 6);
            i7 = composite.decodeIntElement(var2, 7);
            i8 = composite.decodeIntElement(var2, 8);
            i9 = composite.decodeIntElement(var2, 9);
            i10 = composite.decodeIntElement(var2, 10);
            composite.endStructure(var2);
            return new Fields10New(Integer.MAX_VALUE, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10);
        } else {
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
                    case 2:
                        i3 = composite.decodeIntElement(var2, 2);
                        mask |= 8;
                        break;
                    case 3:
                        i4 = composite.decodeIntElement(var2, 3);
                        mask |= 16;
                        break;
                    case 4:
                        i5 = composite.decodeIntElement(var2, 4);
                        mask |= 32;
                        break;
                    case 5:
                        i6 = composite.decodeIntElement(var2, 5);
                        mask |= 64;
                        break;
                    case 6:
                        i7 = composite.decodeIntElement(var2, 6);
                        mask |= 128;
                        break;
                    case 7:
                        i8 = composite.decodeIntElement(var2, 7);
                        mask |= 256;
                        break;
                    case 8:
                        i9 = composite.decodeIntElement(var2, 8);
                        mask |= 512;
                        break;
                    case 9:
                        i10 = composite.decodeIntElement(var2, 9);
                        mask |= 1024;
                        break;
                    case -1:
                        composite.endStructure(var2);
                        return new Fields10New(mask, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10);
                    default:
                        throw new RuntimeException();
                }
            }
        }
    }
}

