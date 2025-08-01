package com.example.domain

interface PatternValidator {

    fun matches(value: String): Boolean
}