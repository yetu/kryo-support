package com.yetu.kryo_support;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton to handle Kryo serialization and deserialization
 */
public class KryoSupport {
    private static KryoSupport instance = new KryoSupport();

    /**
     * Initial buffer size for marshalling
     */
    private static final int BUFFER_SIZE = 512;

    /**
     * Map of registered classes and their IDs
     */
    private Map<Class<? extends Message>, Integer> registrations = null;

    /**
     * Set to true upon the creation of the first Kryo instance in the pool to ensure that IDs are always coherent
     */
    private boolean registrationFinished = false;

    /**
     * Factory for the KryoPool to ensure all Kryo instances are correctly configured
     */
    private final KryoFactory factory = new KryoFactory() {
        @Override
        public Kryo create () {
            // Upon the creation of the first Kryo instance the registration must be frozen to ensure IDs are always coherent
            registrationFinished = true;

            Kryo kryo = new Kryo();

            // configure kryo instance, customize settings

            kryo.setRegistrationRequired(true);
            kryo.setDefaultSerializer(TaggedFieldSerializer.class);

            // Register all registered classes with Kryo
            for (Class<? extends Message> klazz : registrations.keySet()) {
                kryo.register(klazz, registrations.get(klazz));
            }

            return kryo;
        }
    };

    // Build pool with SoftReferences enabled
    private KryoPool pool = null;

    private KryoSupport() {
        initializeKryo();
    }

    /**
     * Initialize (or re-initialize) the KryoSupport class
     */
    private void initializeKryo() {
        pool = new KryoPool.Builder(factory).softReferences().build();
        registrations = new HashMap<>();
        registrationFinished = false;
    }

    /**
     * Register a class with Kryo. All registrations must be completed before the first access to Kryo.
     * @param klazz The class to register
     * @param id The ID to assign to the class
     * @throws IllegalStateException if the registration is already frozen
     * @throws IllegalArgumentException if the class or ID has already been registered
     */
    private void registerClass(Class<? extends Message> klazz, int id) {
        if (registrationFinished) {
            throw new IllegalStateException("Attempted to register class " + klazz + "After first use of Kryo. All classes must be registered before the first use.");
        }

        if (registrations.containsValue(id)) {
            throw new IllegalArgumentException("Duplicate ID " + id + " for class " + klazz);
        }

        if (registrations.containsKey(klazz)) {
            throw new IllegalArgumentException("Reregistering class " + klazz + " with ID " + id + " (old ID: " + registrations.get(klazz) + ")");
        }

        registrations.put(klazz, id);
    }

    /**
     * Marshall a message into a byte Array
     * @param message The message to marshalled
     * @return The marshalled bytes
     */
    private byte[] marshallMessage (Message message) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(BUFFER_SIZE);
        Output output = new UnsafeOutput(outputStream);
        Kryo kryo = pool.borrow();
        kryo.writeClassAndObject(output, message);
        output.close();
        pool.release(kryo);
        return outputStream.toByteArray();
    }

    /**
     * Initialize the Singleton.
     */
    public static void initialize() {
        instance.initializeKryo();
    }

    /**
     * Singleton pattern: get the Kryo instance
     * @return The singleton instance
     */
    public static KryoSupport get() {
        return instance;
    }


    /**
     * Convenience method. Calls registerClass on the Kryo singleton
     * @param klazz The class to register
     * @param id The ID to assign to the class
     * @throws IllegalStateException if the registration is already frozen
     * @throws IllegalArgumentException if the class or ID has already been registered
     */
    public static void register(Class<? extends Message> klazz, int id) {
        KryoSupport.get().registerClass(klazz, id);
    }

    /**
     * Convenience method. Calls marshallMessage on the Kryo Singleton
     * @param message The message to be marshalled
     * @return The marshalled bytes
     */
    public static byte[] marshall (Message message) {
        return instance.marshallMessage(message);
    }

    /**
     * Convenience method. Unmarshall a byte Array into a Message
     * @param bytes The bytes to unmarshal
     * @return The unmarshalled message
     */
    public static Message unmarshall(byte[] bytes) {
        Input input = new UnsafeInput(bytes);
        return instance.unmarshallMessage(input);
    }

    /**
     * Convenience method. Unmarshall a Java NIO ByteBuffer into a Message
     * @param buffer The ByteBuffer containing the bytes to unmarshall
     * @return The unmarshalled Message
     */
    public static Message unmarshall(ByteBuffer buffer) {
        Input input = new UnsafeMemoryInput(buffer);
        return instance.unmarshallMessage(input);
    }

    /**
     * Unmarshall a Kryo Input into a buffer
     * @param input The Kryo Input containing the bytes to unmarshall
     * @return The unmarshalled Message
     */
    private Message unmarshallMessage(Input input) {
        Kryo kryo = pool.borrow();
        Message message = (Message)kryo.readClassAndObject(input);
        pool.release(kryo);

        return message;
    }
}
