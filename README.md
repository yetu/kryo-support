# kryo-support

This is a small library to make the [Kryo](https://github.com/EsotericSoftware/kryo) serialization framework easier
to use. By using this we ensure that:

* All projects use the same Kryo settings
* All projects depend on the same version of Kryo

Usage:

```java
import com.esotericsoftware.kryo.serializers.TaggedFieldSerializer.Tag;
import com.yetu.kryo_support.KryoSupport;
import com.yetu.kryo_support.Message;

public class TestMessage extends Message { // All messages must extend the Message class;
    @Tag(1) int field1; // This field will be serialized (note the @Tag annotation). Tag IDs must be unique within each class.
    int field2; // This field will not be serialized
    
    private TestMessage() { } // There must be a default constructor
    
    public TestMessage (int field1, int field2) {
        this.field1 = field1;
        this.field2 = field2;
    }
}

public class Example {
    KryoSupport.register(TestMessage.class, 1); // ID numbers must be unique
    
    TestMessage message = new TestMessage(42, 4242);
    
    byte[] bytes = KryoSupport.marshall(message);
    TestMessage result = (TestMessage)KryoSupport.unmarshall(bytes);
}
```

For more examples, look at the tests.
