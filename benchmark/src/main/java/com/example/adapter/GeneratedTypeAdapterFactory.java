package com.example.adapter;

import com.google.gson.TypeAdapterFactory;
import com.ryanharter.auto.value.gson.GsonTypeAdapterFactory;

@GsonTypeAdapterFactory
public abstract class GeneratedTypeAdapterFactory implements TypeAdapterFactory {

    public static GeneratedTypeAdapterFactory create() {
        return new AutoValueGson_GeneratedTypeAdapterFactory();
    }
}
