package com.kosmos.bootstrapper.exception

class DuplicateDomainException(message: String? = null, e: Throwable? = null): Exception(message, e)
class CircularDependencyException(message: String? = null, e: Throwable? = null): Exception(message, e)
class PluginInstantiationException(message: String? = null, e: Throwable? = null): Exception(message, e)