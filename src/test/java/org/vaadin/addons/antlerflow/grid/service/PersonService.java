package org.vaadin.addons.antlerflow.grid.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.vaadin.addons.antlerflow.grid.entity.PersonEntity;
import org.vaadin.addons.antlerflow.grid.filter.PersonFilter;
import org.vaadin.addons.antlerflow.grid.model.Person;
import org.vaadin.addons.antlerflow.grid.repository.PersonRepository;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;

    @PostConstruct
    private void init() {
        repository.saveAll(
                generatePeople(500).stream()
                        .map(
                                person ->
                                        PersonEntity.builder()
                                                .firstName(person.getFirstName())
                                                .lastName(person.getLastName())
                                                .age(person.getAge())
                                                .build())
                        .toList());
    }

    public Page<Person> search(Optional<PersonFilter> filter, Pageable pageable) {
        Page<PersonEntity> entityPage;
        if (filter.isPresent()) {
            entityPage = repository.findAll(filter.get(), pageable);
        } else {
            entityPage = repository.findAll(pageable);
        }
        return entityPage.map(
                entity ->
                        Person.builder()
                                .id(entity.getId())
                                .firstName(entity.getFirstName())
                                .lastName(entity.getLastName())
                                .age(entity.getAge())
                                .build());
    }

    public List<Person> generatePeople(int count) {
        String[] firstNames = {
            "Liam", "Olivia", "Noah", "Emma", "Oliver", "Ava", "Elijah", "Sophia", "James",
                    "Isabella",
            "William", "Mia", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn",
                    "Theodore", "Charlotte",
            "Alexander", "Scarlett", "Jackson", "Ella", "Mateo", "Grace", "Michael", "Chloe",
                    "Daniel", "Camila",
            "Logan", "Luna", "Sebastian", "Sofia", "Jack", "Layla", "Aiden", "Aria", "Owen", "Mila",
            "Samuel", "Nora", "Matthew", "Hazel", "Joseph", "Riley", "Levi", "Zoe", "David", "Lily"
        };

        String[] lastNames = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
                    "Rodriguez", "Martinez",
            "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore",
                    "Jackson", "Martin",
            "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis",
                    "Robinson",
            "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill",
                    "Flores",
            "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter",
                    "Roberts"
        };

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<Person> list = new ArrayList<>(count);

        for (int i = 1; i <= count; i++) {
            String first = firstNames[rnd.nextInt(firstNames.length)];
            String last = lastNames[rnd.nextInt(lastNames.length)];

            int age = rnd.nextInt(18, 66); // 18–65

            list.add(new Person((long) i, first, last, age));
        }
        return list;
    }
}
