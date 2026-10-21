package sg.edu.nus.iss.d13revision.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sg.edu.nus.iss.d13revision.models.Person;

class PersonServiceTest {

    private PersonService personService;

    @BeforeEach
    void setUp() {
        personService = new PersonService();
    }

    @Test
    void personCrudLifecyclePreservesExistingPeopleAndCopiesInput() {
        // Read the initial records.
        List<Person> originalPersons = List.copyOf(personService.getPersons());
        assertThat(originalPersons).extracting(Person::getFirstName)
                .containsExactly("Mark", "Elon");
        assertThat(originalPersons).extracting(Person::getLastName)
                .containsExactly("Zuckerberg", "Musk");
        assertThat(originalPersons).allSatisfy(person -> assertThat(person.getId()).isNotBlank());
        assertThat(originalPersons).extracting(Person::getId).doesNotHaveDuplicates();

        // Create a person, then read it back.
        Person input = new Person("submitted-id", "Jane", "Doe");
        personService.addPerson(input);
        input.setFirstName("Changed");
        input.setLastName("Name");

        assertThat(personService.getPersons()).hasSize(3).containsAll(originalPersons);
        Person added = personService.getPersons().get(2);
        String id = added.getId();
        assertThat(id).isNotBlank().isNotEqualTo(input.getId());
        assertThat(added).isNotSameAs(input);
        assertThat(added.getFirstName()).isEqualTo("Jane");
        assertThat(added.getLastName()).isEqualTo("Doe");
        assertThat(personService.getPersons()).extracting(Person::getId).doesNotHaveDuplicates();

        // Update by ID using a separate object, then read the updated record.
        Person update = new Person(id, "Janet", "Smith");
        personService.updatePerson(update);
        update.setId("changed-id");
        update.setFirstName("Changed");
        update.setLastName("Name");

        assertThat(personService.getPersons()).hasSize(3)
                .containsAll(originalPersons).doesNotContain(added);
        assertThat(personService.getPersons()).filteredOn(person -> person.getId().equals(id))
                .singleElement().satisfies(person -> {
                    assertThat(person).isNotSameAs(update);
                    assertThat(person.getFirstName()).isEqualTo("Janet");
                    assertThat(person.getLastName()).isEqualTo("Smith");
                });

        // Delete by ID, even when the supplied names differ.
        personService.removePerson(new Person(id, "Different", "Name"));

        assertThat(personService.getPersons()).containsExactlyElementsOf(originalPersons);
    }

    @Test
    void personCrudLifecycleHandlesUnknownIdsAndAnEmptyList() {
        List<Person> originalPersons = List.copyOf(personService.getPersons());

        // Create and read a person before exercising unknown IDs.
        personService.addPerson(new Person("Jane", "Doe"));
        assertThat(personService.getPersons()).hasSize(3).containsAll(originalPersons);
        List<Person> beforeUnknownDelete = List.copyOf(personService.getPersons());

        // An unknown ID must not delete a person with matching names.
        personService.removePerson(new Person("unknown-id", "Mark", "Zuckerberg"));
        assertThat(personService.getPersons()).containsExactlyElementsOf(beforeUnknownDelete);

        // The current service adds a new person when updating an unknown ID.
        personService.updatePerson(new Person("unknown-id", "Janet", "Smith"));
        assertThat(personService.getPersons()).hasSize(4).containsAll(beforeUnknownDelete);
        assertThat(personService.getPersons()).filteredOn(person -> person.getId().equals("unknown-id"))
                .singleElement().satisfies(person -> {
                    assertThat(person.getFirstName()).isEqualTo("Janet");
                    assertThat(person.getLastName()).isEqualTo("Smith");
                });

        // Delete all records and confirm repeated deletion is harmless.
        List<Person> personsToDelete = List.copyOf(personService.getPersons());
        personsToDelete.forEach(personService::removePerson);
        personService.removePerson(personsToDelete.get(0));

        assertThat(personService.getPersons()).isEmpty();
    }
}
