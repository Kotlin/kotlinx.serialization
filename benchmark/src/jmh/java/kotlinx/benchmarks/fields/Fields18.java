package kotlinx.benchmarks.fields;

import kotlin.jvm.internal.Intrinsics;
import kotlinx.serialization.CompositeDecoder;
import kotlinx.serialization.Decoder;
import kotlinx.serialization.KSerializer;
import kotlinx.serialization.SerialDescriptor;

public class Fields18 {
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
    int i11;
    int i12;
    int i13;
    int i14;
    int i15;
    int i16;
    int i17;
    int i18;

    public Fields18(int mask, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12, int i13, int i14, int i15, int i16, int i17, int i18) {
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
        this.i11 = i11;
        this.i12 = i12;
        this.i13 = i13;
        this.i14 = i14;
        this.i15 = i15;
        this.i16 = i16;
        this.i17 = i17;
        this.i18 = i18;
    }

    public static Fields18 deserialize(Decoder decoder) {
        Intrinsics.checkParameterIsNotNull(decoder, "decoder");
        SerialDescriptor var2 = null;
        boolean readAll = false;
        int mask = 0;
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
        int i11 = 0;
        int i12 = 0;
        int i13 = 0;
        int i14 = 0;
        int i15 = 0;
        int i16 = 0;
        int i17 = 0;
        int i18 = 0;
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
                case 1:
                    i2 = composite.decodeIntElement(var2, 1);
                    mask |= 2;
                    if (!readAll) {
                        break;
                    }
                case 2:
                    i3 = composite.decodeIntElement(var2, 2);
                    mask |= 4;
                    if (!readAll) {
                        break;
                    }
                case 3:
                    i4 = composite.decodeIntElement(var2, 3);
                    mask |= 8;
                    if (!readAll) {
                        break;
                    }
                case 4:
                    i5 = composite.decodeIntElement(var2, 4);
                    mask |= 16;
                    if (!readAll) {
                        break;
                    }
                case 5:
                    i6 = composite.decodeIntElement(var2, 5);
                    mask |= 32;
                    if (!readAll) {
                        break;
                    }
                case 6:
                    i7 = composite.decodeIntElement(var2, 6);
                    mask |= 64;
                    if (!readAll) {
                        break;
                    }
                case 7:
                    i8 = composite.decodeIntElement(var2, 7);
                    mask |= 128;
                    if (!readAll) {
                        break;
                    }
                case 8:
                    i9 = composite.decodeIntElement(var2, 8);
                    mask |= 256;
                    if (!readAll) {
                        break;
                    }
                case 9:
                    i10 = composite.decodeIntElement(var2, 9);
                    mask |= 512;
                    if (!readAll) {
                        break;
                    }
                case 10:
                    i11 = composite.decodeIntElement(var2, 10);
                    mask |= 1024;
                    if (!readAll) {
                        break;
                    }
                case 11:
                    i12 = composite.decodeIntElement(var2, 11);
                    mask |= 2048;
                    if (!readAll) {
                        break;
                    }
                case 12:
                    i13 = composite.decodeIntElement(var2, 12);
                    mask |= 4096;
                    if (!readAll) {
                        break;
                    }
                case 13:
                    i14 = composite.decodeIntElement(var2, 13);
                    mask |= 8192;
                    if (!readAll) {
                        break;
                    }
                case 14:
                    i15 = composite.decodeIntElement(var2, 14);
                    mask |= 16384;
                    if (!readAll) {
                        break;
                    }
                case 15:
                    i16 = composite.decodeIntElement(var2, 15);
                    mask |= 32768;
                    if (!readAll) {
                        break;
                    }
                case 16:
                    i17 = composite.decodeIntElement(var2, 16);
                    mask |= 65536;
                    if (!readAll) {
                        break;
                    }
                case 17:
                    i18 = composite.decodeIntElement(var2, 17);
                    mask |= 131072;
                    if (!readAll) {
                        break;
                    }
                case -1:
                    composite.endStructure(var2);
                    return new Fields18(mask, i1, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12, i13, i14, i15, i16, i17, i18);
                default:
                    throw new RuntimeException();
            }
        }
    }
}

