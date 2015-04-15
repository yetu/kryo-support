package com.yetu.kryo_support;

import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

class TestMessage implements Message {
    @Tag(1) private int value;

    private TestMessage() { }

    public TestMessage(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestMessage that = (TestMessage) o;

        return getValue() == that.getValue();

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getValue() + ")";
    }
}

class TestComplexMessage implements Message {
    @Tag(1) private TestMessage message1;
    @Tag(2) private TestMessage message2;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestComplexMessage that = (TestComplexMessage) o;

        if (getMessage1() != null ? !getMessage1().equals(that.getMessage1()) : that.getMessage1() != null)
            return false;
        return !(getMessage2() != null ? !getMessage2().equals(that.getMessage2()) : that.getMessage2() != null);

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getMessage1() + "," + getMessage2() + ")";
    }
}

class TestUntaggedMessage implements Message {
    int value;
    TestMessage message;

    private TestUntaggedMessage() { }

    public TestUntaggedMessage(int value, TestMessage message) {
        this.value = value;
        this.message = message;
    }

    public int getValue() {
        return value;
    }

    public TestMessage getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestUntaggedMessage that = (TestUntaggedMessage) o;

        if (getValue() != that.getValue()) return false;
        return !(getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null);

    }

    @Override
    public String toString() {
        return "TestUntaggedMessage(" + value + ", " + message + ')';
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
        KryoSupport.register(TestMessage.class, 1);
        KryoSupport.register(TestComplexMessage.class, 2);
        KryoSupport.register(TestUntaggedMessage.class, 3);
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
        byte[] bytes = KryoSupport.marshall(testMessage1);
        TestMessage result = (TestMessage)KryoSupport.unmarshall(bytes);

        assertEquals(testMessage1, result);
    }

    @Test
    public void testMarshallAndUnmarshallComplex() throws Exception {
        register();

        byte[] bytes = KryoSupport.marshall(complexMessage);
        TestComplexMessage unmarshallResult = (TestComplexMessage)KryoSupport.unmarshall(bytes);
        assertEquals(complexMessage, unmarshallResult);
    }

    @Test
    public void testMarshallAndUnmarshallSameObject() throws Exception {
        register();
        TestComplexMessage complexMessage2 = new TestComplexMessage(testMessage1, testMessage1);

        byte[] bytes = KryoSupport.marshall(complexMessage2);
        TestComplexMessage result = (TestComplexMessage)KryoSupport.unmarshall(bytes);
        assertEquals(complexMessage2, result);
        assertSame(result.getMessage1(), result.getMessage2());
    }

    @Test
    public void testMarshallAndUnmarshallDifferentKryos() throws Exception {
        register();

        byte[] bytes = KryoSupport.marshall(complexMessage);

        initialize();
        register();

        TestComplexMessage result = (TestComplexMessage)KryoSupport.unmarshall(bytes);
        assertEquals(complexMessage, result);
    }

    @Test
    public void testUntagged() throws Exception {
        register();

        TestUntaggedMessage message = new TestUntaggedMessage(42, new TestMessage(42));
        byte[] bytes = KryoSupport.marshall(message);
        TestUntaggedMessage result = (TestUntaggedMessage)KryoSupport.unmarshall(bytes);

        assertEquals(0, result.getValue());
        assertNull(result.getMessage());
    }
}