package com.thisaay.domainer


@DomainModel(Student::class)
data class DBStudent(
    @DomainName("username")
    val user_name: String,
)

data class Student(
    val username: String,
)
