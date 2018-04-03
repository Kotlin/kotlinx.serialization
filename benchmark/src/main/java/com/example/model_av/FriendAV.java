package com.example.model_av;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

@AutoValue
public abstract class FriendAV {

    public abstract int id();

    public abstract String name();

    public static JsonAdapter<FriendAV> jsonAdapter(Moshi moshi) {
        return new AutoValue_FriendAV.MoshiJsonAdapter(moshi);
    }

    public static TypeAdapter<FriendAV> typeAdapter(Gson gson) {
        return new AutoValue_FriendAV.GsonTypeAdapter(gson);
    }
}
