package com.example.model_av;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class NameAV {

    public abstract String first();

    public abstract String last();

    public static JsonAdapter<NameAV> jsonAdapter(Moshi moshi) {
        return new AutoValue_NameAV.MoshiJsonAdapter(moshi);
    }

    public static TypeAdapter<NameAV> typeAdapter(Gson gson) {
        return new AutoValue_NameAV.GsonTypeAdapter(gson);
    }
}
