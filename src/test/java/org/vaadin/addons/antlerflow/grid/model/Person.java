package org.vaadin.addons.antlerflow.grid.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Person {
    private Long id;
    private String firstName;
    private String lastName;
    private int age;
}
