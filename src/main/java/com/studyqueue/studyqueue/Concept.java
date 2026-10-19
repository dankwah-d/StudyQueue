package com.studyqueue.studyqueue;

import jakarta.persistence.*;

@Entity
@Table(name = "concepts")
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected Concept() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
