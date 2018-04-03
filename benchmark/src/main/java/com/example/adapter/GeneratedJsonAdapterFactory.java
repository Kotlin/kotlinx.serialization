package com.example.adapter;

import com.ryanharter.auto.value.moshi.MoshiAdapterFactory;
import com.squareup.moshi.JsonAdapter;

@MoshiAdapterFactory
public abstract class GeneratedJsonAdapterFactory implements JsonAdapter.Factory {

    public static GeneratedJsonAdapterFactory create() {
        return new AutoValueMoshi_GeneratedJsonAdapterFactory();
    }
}
