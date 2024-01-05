package com.tdder.junit.jupiter.extension.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.tdder.junit.jupiter.extension.TeardownExtension;
import com.tdder.junit.jupiter.extension.TeardownRegistry;

// Annotate TeardownExtension
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

    static Student createStudent(final String name) {
        return new Student(name);
    }

    static void deleteStudent(final Student student) {
        System.out.println("deleteStudent: " + student.getName());
    }

    static Course createCourse(final String name) {
        return new Course(name);
    }

    static void deleteCourse(final Course course) {
        System.out.println("deleteStudent: " + course.getName());
    }

}
