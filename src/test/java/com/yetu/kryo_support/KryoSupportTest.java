package com.yetu.kryo_support;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

class TestMessage implements Message {
    private int value;

    private TestMessage() { }

    public TestMessage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

class TestComplexMessage implements Message {
    private TestMessage message1;
    private TestMessage message2;

    private TestComplexMessage() { }

    public TestComplexMessage(TestMessage message2, TestMessage message1) {
        this.message2 = message2;
        this.message1 = message1;
    }

    public TestMessage getMessage1() {
        return message1;
    }

    public TestMessage getMessage2() {
        return message2;
    }
}

public class KryoSupportTest {
    TestMessage testMessage1 = new TestMessage(42);
    TestMessage testMessage2 = new TestMessage(4242);
    TestComplexMessage complexMessage = new TestComplexMessage(testMessage1, testMessage2);

    @Before
    public void initialize() {
        KryoSupport.initialize();
    }

    private void register() {
        KryoSupport.register(TestComplexMessage.class, 1);
        KryoSupport.register(TestMessage.class, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerDuplicateClass() throws Exception {
        KryoSupport.register(TestMessage.class, 1);
        KryoSupport.register(TestMessage.class, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerDuplicateId() throws Exception {
        KryoSupport.register(TestMessage.class, 1);
        KryoSupport.register(TestComplexMessage.class, 1);
    }


    @Test(expected = IllegalStateException.class)
    public void registerAfterFirstAccess() throws Exception {
        KryoSupport.register(TestMessage.class, 2);
        KryoSupport.marshall(new TestMessage(42));
        KryoSupport.register(TestComplexMessage.class, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void marshallUnregistered() throws Exception {
        KryoSupport.marshall(new TestMessage(42));
    }

    @Test
    public void testMarshallAndUnmarshall() throws Exception {
        register();
        byte[] result = KryoSupport.marshall(testMessage1);
        TestMessage unmarshallResult = (TestMessage)KryoSupport.unmarshall(result);

        assertEquals(unmarshallResult.getValue(), testMessage1.getValue());
    }

    @Test
    public void testMarshallAndUnmarshallComplex() throws Exception {
        register();

        byte[] result = KryoSupport.marshall(complexMessage);
        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(result);

        TestMessage result1 = unmarshallResult.getMessage1();
        TestMessage result2 = unmarshallResult.getMessage2();

        assertEquals(result1.getValue(), testMessage1.getValue());
        assertEquals(result2.getValue(), testMessage2.getValue());
    }

    @Test
    public void testMarshallAndUnmarshallDifferentKryos() throws Exception {
        register();
        byte[] result = KryoSupport.marshall(complexMessage);

        initialize();
        register();

        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(result);

        TestMessage result1 = unmarshallResult.getMessage1();
        TestMessage result2 = unmarshallResult.getMessage2();

        assertEquals(result1.getValue(), testMessage1.getValue());
        assertEquals(result2.getValue(), testMessage2.getValue());
    }
}