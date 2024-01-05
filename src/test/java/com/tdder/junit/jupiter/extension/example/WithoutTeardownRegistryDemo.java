package com.tdder.junit.jupiter.extension.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WithoutTeardownRegistryDemo {

    @Test
    void testWithoutTeardownRegistry() throws Exception {
        // === Setup ===
        final Student bob = createStudent("bob");
        try {
            final Course course = createCourse("My Course");
            try {
                // === Exercise ===
                course.register(bob);

                // === Verify ===
                assertEquals(1, course.numberOfStudents());
            } finally {
                deleteCourse(course);
            }
        } finally {
            deleteStudent(bob);
        }
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
