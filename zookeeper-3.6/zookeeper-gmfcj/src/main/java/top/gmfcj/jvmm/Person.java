package top.gmfcj.jvmm;

public class Person {

    private Integer id;
    private String name;

    Person(Integer id,String name){
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;

        Person person = (Person) o;

        if (id != null ? !id.equals(person.id) : person.id != null) return false;
        return name != null ? name.equals(person.name) : person.name == null;
    }
}
