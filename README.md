# Teardown Extension

TeardownExtension is an extension for JUnit Jupiter. Provides _Automated Teardown_ mechanism.

- Make it easier for developers to read and write teardown code.
- Ensuring the test fixtures are torn down.
- Inspired by xUTP's [Automated Teardown](http://xunitpatterns.com/Automated%20Teardown.html).

## Usage

1. Annotate a test class with `TeardownExtension`. e.g `@ExtendWith(TeardownExtension.class) `
1. Declare `TeardownRegistry` parameter or field.
1. Register teardown code to `TeardownRegistry#add`.

If you register codes to `TeardownRegistry`, automatically execute codes at the end of the test.

Once you have created a fixture that requires cleanup, you can register the cleanup code to `TeardownRegistry`.
If multiple codes are registered, they will be executed in the reverse order of registration.

```java
@ExtendWith(TeardownExtension.class) 
class TeardownExampleTest {

    @Test
    void someTest(final TeardownRegistry teardownRegistry) {
        final FooFixture fixture1 = createFooFixture();
        teardownRegistry.add(() -> fixture1.cleanup());

        // If BarFixture implements java.lang.AutoCloseable, you can directly add.
        final BarFixture fixture2 = teardownRegistry.add(createBarFixture());
        
        // ...

        // After test method, fixture2.close() and fixture1.cleanup() are executed.
    }

}
```

### How to get `TeardownRegistry`

You can use method injection and field injection.

```java
@ExtendWith(TeardownExtension.class)
class InjectionDemo {

    // Registered codes are executed after @AfterAll
    private static TeardownRegistry staticTeardownRegistry;

    // Registered codes are executed after @AfterEach
    private TeardownRegistry instanceTeardownRegistry;

    @BeforeAll
    static void beforeAll(final TeardownRegistry teardownRegistry1) {
        // Registered codes are executed after @AfterAll
        // ...
    }

    @BeforeEach
    void setUp(final TeardownRegistry teardownRegistry) {
        // Registered codes are executed after @AfterEach
        // ...
    }

    @Test
    void someTest(final TeardownRegistry teardownRegistry) {
        // Registered codes are executed after @AfterEach
        // ...
    }

}
```


## Example

Example usage:

```java
// Annotate a test class with TeardownExtension
@ExtendWith(TeardownExtension.class)
class WithTeardownRegistryDemo {

    /*
     * Use teardownRegistry field in test methods and @BeforeEach or @AfterEach lifecycle methods.
     *
     * This field is injected by TeardownExtension.
     */
    private TeardownRegistry teardownRegistry;

    @Test
    void testWithTeardownRegistry() throws Exception {
        // === Setup ===

        final Student alice = createStudent("alice");
        // Registered code is executed after @AfterEach
        teardownRegistry.add(() -> deleteStudent(alice));

        final Course course = createCourse("TDD");
        // Registered code is executed after @AfterEach
        teardownRegistry.add(() -> deleteCourse(course));

        // === Exercise ===

        course.register(alice);

        // === Verify ===

        assertEquals(1, course.numberOfStudents());

        // === Teardown ===

        /*
         * After tests, either succeeded or failure, all students and courses is deleted.
         *
         * The order is in reverse order of addition to the TeardownRegistry, as follows:
         * 1. deleteCourse(course)
         * 2. deleteStudent(alice)
         */
    }

}
```

## Supported Java Versions

Java 8.

## Dependency

Gradle:

```
implementation 'com.tdder.junit:junit-teardown-extension:1.0.0'
```

Maven:

```xml
<dependency>
    <groupId>com.tdder.junit</groupId>
    <artifactId>junit-teardown-extension</artifactId>
    <version>1.0.0</version>
</dependency>
```

For other tools, see [Central Repository](https://central.sonatype.com/artifact/com.tdder.junit/junit-teardown-extension).

Not mentioned above, but of course dependency on junit jupiter is required.

## License

Licensed under the ASL2 license.
