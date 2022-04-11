# Domainer
Plugin for generating auto mappers for clean arch domain data model built using KSP


This library has been develped in two days as proof of concept, So it needs a lot of work, and defently a rewrite.


# How to use

Anotate your DB or api data class with @DomainModel and give it the domain data class

```kotlin
  @DomainModel(Student::class)
  data class DBStudent(
    val name: String,
    val grade: Int,
  )

  data class Student(
    val name: String,
    val grade: Int,
  )
```

This will generate this code
```kotlin
fun DBStudent.toModel(): Student = Student(
    name = name,
    grade = grade,
)

fun Student.toData(): DBStudent = DBStudent(
    name = name,
    grade = grade,
)
```

The lib also supports enum ordinal using @EnumOrdinal:
```kotlin
@DomainModel(Student::class)
data class DBStudent(
    val name: String,
    val grade: Int,
    @EnumOrdinal(DBType::class)
    val type: Int,
)

data class Student(
    val name: String,
    val grade: Int,
    val type: Type,
)

@DomainModel(Type::class)
enum class DBType {
    FIRST, SECOND
}

enum class Type {
    FIRST, SECOND
}
```

This will generate this code:

```kotlin
fun DBStudent.toModel(): Student = Student(
    name = name,
    grade = grade,
    type = DBType.values()[type].toModel(),
)

fun Student.toData(): DBStudent
 = DBStudent(
    name = name,
    grade = grade,
    type = type.toData().ordinal,
)

fun DBType.toModel(): Type  = when(this) {
    DBType.FIRST -> Type.FIRST
    DBType.SECOND -> Type.SECOND
}

fun Type.toData(): DBType  = when(this) {
    Type.FIRST -> DBType.FIRST 
    Type.SECOND -> DBType.SECOND 
}
```

One last thing, if you have a different name for an attrivute in the domain model you can use @DomainName:

```kotlin
@DomainModel(Student::class)
data class DBStudent(
    @DomainName("username")
    val user_name: String,
)

data class Student(
    val username: String,
)
```

This will generate this code:
```kotlin
fun DBStudent.toModel(): Student = Student(
    username = user_name,
)

fun Student.toData(): DBStudent = DBStudent(
    user_name = username,
)
```

TODO list:
- [ ] Publish the library
- [ ] Rewrite the whole plugin
- [ ] Add Support for mapping collections
- [ ] Add Support for maping dates

