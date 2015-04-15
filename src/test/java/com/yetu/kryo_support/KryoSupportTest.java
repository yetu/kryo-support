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

    @Override
    public boolean equals(Object that) {
        return that.getClass() == this.getClass() && ((TestMessage)that).getValue() == getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getValue() + ")";
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

    @Override
    public boolean equals(Object that) {
        return that.getClass() == this.getClass() &&
                ((TestComplexMessage)that).getMessage1() == getMessage1() &&
                ((TestComplexMessage)that).getMessage2() == getMessage2();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getMessage1() + "," + getMessage2() + ")";
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

        assertEquals(testMessage1, unmarshallResult);
    }

    @Test
    public void testMarshallAndUnmarshallComplex() throws Exception {
        register();

        byte[] result = KryoSupport.marshall(complexMessage);
        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(result);
        assertEquals(complexMessage, unmarshallResult);
    }

    @Test
    public void testMarshallAndUnmarshallSameObject() throws Exception {
        register();
        TestComplexMessage complexMessage2 = new TestComplexMessage(testMessage1, testMessage1);

        byte[] result = KryoSupport.marshall(complexMessage2);
        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(result);
        assertEquals(complexMessage2, unmarshallResult);
        assertSame(complexMessage2.getMessage1(), complexMessage2.getMessage2());
    }

    @Test
    public void testMarshallAndUnmarshallDifferentKryos() throws Exception {
        register();

        byte[] result = KryoSupport.marshall(complexMessage);

        initialize();
        register();

        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(result);
        assertEquals(complexMessage, unmarshallResult);
    }
}