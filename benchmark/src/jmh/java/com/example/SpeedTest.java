package com.example;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.example.adapter.GeneratedJsonAdapterFactory;
import com.example.adapter.GeneratedTypeAdapterFactory;
import com.example.model_av.ResponseAV;
import com.example.model_reflective.Response;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.moshi.Moshi;

import org.objenesis.strategy.StdInstantiatorStrategy;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;

public class SpeedTest {

    @State(Scope.Benchmark)
    public static class KSerializer {

        @Setup()
        public void doSetup() throws Exception {
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = Response.parse(json);
        }

        public String json;
        public Response response;
    }

    @State(Scope.Benchmark)
    public static class ReflectiveMoshi {

        @Setup
        public void setupTrial() throws Exception {
            moshi = new Moshi.Builder().build();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = moshi.adapter(Response.class).fromJson(json);
        }

        public Moshi moshi;
        public String json;
        public Response response;
    }

    @State(Scope.Benchmark)
    public static class ReflectiveGson {

        @Setup
        public void setupTrial() throws Exception {
            gson = new GsonBuilder().create();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = gson
                    .fromJson(json, Response.class);
        }

        public Gson gson;
        public String json;
        public Response response;
    }

    @State(Scope.Benchmark)
    public static class AVMoshi {

        @Setup
        public void setupTrial() throws Exception {
            moshi = new Moshi.Builder()
                    .add(GeneratedJsonAdapterFactory.create())
                    .build();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = moshi
                    .adapter(ResponseAV.class)
                    .fromJson(json);
        }

        public Moshi moshi;
        public String json;
        public ResponseAV response;
    }

    @State(Scope.Benchmark)
    public static class AVMoshiBuffer {

        @Setup
        public void setupTrial() throws Exception {
            moshi = new Moshi.Builder()
                    .add(GeneratedJsonAdapterFactory.create())
                    .build();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);

            response = moshi
                    .adapter(ResponseAV.class)
                    .fromJson(json);
        }

        @Setup(Level.Invocation)
        public void setupIteration() {
            bufferedSource = new Buffer().writeUtf8(json);
            bufferedSink = new Buffer();
        }

        public Moshi moshi;
        public String json;
        public BufferedSource bufferedSource;
        public BufferedSink bufferedSink;
        public ResponseAV response;
    }

    @State(Scope.Benchmark)
    public static class AVGson {

        @Setup
        public void setupTrial() throws Exception {
            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(GeneratedTypeAdapterFactory.create())
                    .create();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = gson
                    .fromJson(json, ResponseAV.class);
        }

        public Gson gson;
        public String json;
        public ResponseAV response;
    }

    @State(Scope.Benchmark)
    public static class AVGsonBuffer {

        @Setup
        public void setupTrial() throws Exception {
            gson = new GsonBuilder()
                    .registerTypeAdapterFactory(GeneratedTypeAdapterFactory.create())
                    .create();
            URL url = Resources.getResource("largesample.json");
            json = Resources.toString(url, Charsets.UTF_8);
            response = gson
                    .fromJson(json, ResponseAV.class);
        }

        @Setup(Level.Invocation)
        public void setupIteration() {
            source = new InputStreamReader(new Buffer().writeUtf8(json).inputStream(), Charsets.UTF_8);
            sink = new OutputStreamWriter(new Buffer().outputStream(), Charsets.UTF_8);
        }

        public Gson gson;
        public String json;
        public Reader source;
        public Writer sink;
        public ResponseAV response;
    }

    @State(Scope.Benchmark)
    public static class KryoScope {

        @Setup
        public void setupTrial() throws Exception {
            kryo = new Kryo();
            kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
            DefaultInstantiatorStrategy instantiatorStrategy = new DefaultInstantiatorStrategy();
            instantiatorStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            kryo.setInstantiatorStrategy(instantiatorStrategy);

            URL url = Resources.getResource("largesample.json");
            String json = Resources.toString(url, Charsets.UTF_8);
            response = new GsonBuilder()
                    .registerTypeAdapterFactory(GeneratedTypeAdapterFactory.create())
                    .create()
                    .fromJson(json, ResponseAV.class);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            Output output = new Output(stream);
            kryo.writeObject(output, response);
            output.flush();
            bytes = stream.toByteArray();
            responseClazz = getAutoValueClass(ResponseAV.class);
        }

        public Kryo kryo;
        public byte[] bytes;
        public ResponseAV response;
        public Class<ResponseAV> responseClazz;
    }
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String kserializer_toJson(KSerializer param) {
        return param.response.stringify();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String moshi_reflective_toJson(ReflectiveMoshi param) {
        return param.moshi.adapter(Response.class).toJson(param.response);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String moshi_streaming_toJson(AVMoshi param) {
        return param.moshi.adapter(ResponseAV.class).toJson(param.response);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public BufferedSink moshi_streaming_toJson_buffer(AVMoshiBuffer param) throws Exception {
        param.moshi.adapter(ResponseAV.class).toJson(param.bufferedSink, param.response);
        return param.bufferedSink;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String gson_reflective_toJson(ReflectiveGson param) {
        return param.gson.toJson(param.response);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public String gson_streaming_toJson(AVGson param) {
        return param.gson.toJson(param.response);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public Writer gson_streaming_toJson_buffer(AVGsonBuffer param) {
        param.gson.toJson(param.response, param.sink);
        return param.sink;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public byte[] kryo_toBytes(KryoScope param) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Output output = new Output(stream);
        param.kryo.writeObject(output, param.response);
        output.flush();
        return stream.toByteArray();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public Response kserializer_fromJson(KSerializer param) {
        return Response.parse(param.json);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public Response moshi_reflective_fromJson(ReflectiveMoshi param) throws Exception {
        return param.moshi.adapter(Response.class).fromJson(param.json);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public ResponseAV moshi_streaming_fromJson(AVMoshi param) throws Exception {
        return param.moshi.adapter(ResponseAV.class).fromJson(param.json);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public ResponseAV moshi_streaming_fromJson_buffer(AVMoshiBuffer param) throws Exception {
        return param.moshi.adapter(ResponseAV.class).fromJson(param.bufferedSource);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public Response gson_reflective_fromJson(ReflectiveGson param) {
        return param.gson.fromJson(param.json, Response.class);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public ResponseAV gson_streaming_fromJson(AVGson param) {
        return param.gson.fromJson(param.json, ResponseAV.class);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public ResponseAV gson_streaming_fromJson_buffer(AVGsonBuffer param) {
        return param.gson.fromJson(param.source, ResponseAV.class);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public ResponseAV kryo_fromBytes(KryoScope param) throws ClassNotFoundException {
        return param.kryo.readObject(new Input(param.bytes), param.responseClazz);
    }

    private static <T> Class<T> getAutoValueClass(Class<T> clazz) throws ClassNotFoundException {
        String pkgName = clazz.getPackage().getName();
        String className = pkgName + ".AutoValue_" + clazz.getSimpleName();
        //noinspection unchecked
        return (Class<T>) Class.forName(className);
    }
}
