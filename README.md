# kryo-support

This is a small library to make the [Kryo](https://github.com/EsotericSoftware/kryo) serialization framework easier
to use. By using this we ensure that:

* All projects use the same Kryo settings
* All projects depend on the same version of Kryo

## Usage

The kryo-support package is published to [Bintray](https://bintray.com/yetu/maven/kryo-support). To be able to use it,
you need to add the repository to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>bintray</id>
    <url>http://dl.bintray.com/yetu/kryo-support</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>
```

Once you have done that you need to add the following dependency:

```xml
  <dependency>
	<groupId>com.yetu</groupId>
	<artifactId>kryo-support</artifactId>
    <version>1.0.0</version>
  </dependency>
```

This is an example of how to use `kryo-support` from within your code:

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
    public static void main(String args[]) {
        KryoSupport.register(TestMessage.class, 1); // ID numbers must be unique
    
        TestMessage message = new TestMessage(42, 4242);
    
        byte[] bytes = KryoSupport.marshall(message);
        TestMessage result = (TestMessage)KryoSupport.unmarshall(bytes);
    }
}
```

For more examples and usage details, look at the tests and the JavaDocs.

## Publication

Publishing to Bintray uses the `maven-release-plugin`. You can learn more at http://veithen.github.io/2013/05/26/github-bintray-maven-release-plugin.html.

In short, once you have added your credentials to your `$HOME/.m2/settings.xml`, you need to do the following:

```
mvn -B release:prepare
mvn release:perform
```

This will only work if the version number in your `pom.xml` is a SNAPSHOT version. It will remove the -SNAPSHOT before
publishing and then increment the release number and add SNAPSHOT to it (i.e., 1.0.0-SNAPSHOT is released as 1.0.0 and
the POM is left with 1.0.1-SNAPSHOT).
