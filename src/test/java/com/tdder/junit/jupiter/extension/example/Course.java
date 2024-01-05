package com.tdder.junit.jupiter.extension.example;

import java.util.ArrayList;
import java.util.List;

class Course {

    private final String name;

    private final List<Student> students = new ArrayList<>();

    public Course(final String name) {
        this.name = name;
    }

    public void register(final Student student) {
        students.add(student);
    }

    public int numberOfStudents() {
        return students.size();
    }

    public String getName() {
        return name;
    }

}
