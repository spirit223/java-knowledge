package cc.sika.kotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SikaKotlinApplication

fun main(args: Array<String>) {
    runApplication<SikaKotlinApplication>(*args)
}
