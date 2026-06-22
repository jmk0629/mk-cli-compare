package com.mkclicompare

import com.mkclicompare.config.AuthProperties
import com.mkclicompare.config.CliProperties
import com.mkclicompare.config.OAuth2Properties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties::class, CliProperties::class, OAuth2Properties::class)
class MkCliCompareApplication

fun main(args: Array<String>) {
	runApplication<MkCliCompareApplication>(*args)
}
