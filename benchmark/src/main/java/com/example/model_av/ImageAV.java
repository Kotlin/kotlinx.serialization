package com.example.model_av;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class ImageAV {

    public abstract String id();

    public abstract String format();

    public abstract String url();

    public abstract String description();

    public static JsonAdapter<ImageAV> jsonAdapter(Moshi moshi) {
        return new AutoValue_ImageAV.MoshiJsonAdapter(moshi);
    }

    public static TypeAdapter<ImageAV> typeAdapter(Gson gson) {
        return new AutoValue_ImageAV.GsonTypeAdapter(gson);
    }

}
