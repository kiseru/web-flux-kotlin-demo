package com.example.webfluxkotlindemo

import kotlinx.coroutines.flow.asFlow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import org.springframework.web.reactive.function.server.*

@SpringBootApplication
class WebFluxKotlinDemoApplication {

    @Bean
    fun routerFunction(handler: PersonHandler): RouterFunction<ServerResponse> {
        return coRouter {
            "/person".nest {
                GET("/{id}", accept(MediaType.APPLICATION_JSON), handler::getPerson)
                GET("", accept(MediaType.APPLICATION_JSON), handler::listPeople)
                POST("", handler::createPerson)
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<WebFluxKotlinDemoApplication>(*args)
}

@Component
class PersonHandler(private val repository: PersonRepository) {

    suspend fun listPeople(request: ServerRequest): ServerResponse {
        val people = repository.allPeople()
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyAndAwait(people)
    }

    suspend fun createPerson(request: ServerRequest): ServerResponse {
        val person = request.awaitBody<Person>()
        repository.savePerson(person)
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .buildAndAwait()
    }

    suspend fun getPerson(request: ServerRequest): ServerResponse {
        val personId = request.pathVariable("id").toInt()
        return repository.getPerson(personId)?.let {
            ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValueAndAwait(it)
        } ?: ServerResponse.notFound().buildAndAwait()
    }
}

@Repository
class PersonRepository {

    private val people = mutableMapOf(
        1 to Person(1, "Some cool name #1"),
        2 to Person(2, "Some cool name #2"),
        3 to Person(3, "Some cool name #3")
    )

    suspend fun allPeople() = people.values.asFlow()

    suspend fun savePerson(person: Person) {
        people[person.id] = person
    }

    suspend fun getPerson(id: Int) = people[id]
}


data class Person(val id: Int, val name: String)
