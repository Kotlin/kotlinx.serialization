package com.example.model_av;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.util.List;

@AutoValue
public abstract class ResponseAV {

    public abstract List<UserAV> users();

    public abstract String status();

    @SerializedName("is_real_json")
    @Json(name = "is_real_json")
    public abstract boolean isRealJson();

    public static JsonAdapter<ResponseAV> jsonAdapter(Moshi moshi) {
        return new AutoValue_ResponseAV.MoshiJsonAdapter(moshi);
    }

    public static TypeAdapter<ResponseAV> typeAdapter(Gson gson) {
        return new AutoValue_ResponseAV.GsonTypeAdapter(gson);
    }
}
