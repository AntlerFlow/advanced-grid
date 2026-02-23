package org.vaadin.addons.antlerflow.grid.filter;

import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.frontendtools.internal.commons.lang3.Strings;
import lombok.Data;
import org.vaadin.addons.antlerflow.grid.model.Person;

@Data
public class PersonFilter {
    private String name;
    private Integer ageGreaterEqual;
    private Integer ageLessEqual;

    public SerializablePredicate<Person> getPredicate() {
        return  person -> {
            boolean nameMatched = name == null || name.isBlank() || Strings.CI.contains(person.getFirstName(), name)
                    || Strings.CI.contains(person.getLastName(), name);
            boolean ageGreaterEqualMatched = ageGreaterEqual == null || person.getAge() >= ageGreaterEqual;
            boolean ageLessEqualMatched = ageLessEqual == null || person.getAge() <= ageLessEqual;
            return nameMatched && ageGreaterEqualMatched & ageLessEqualMatched;
        };
    }
}
