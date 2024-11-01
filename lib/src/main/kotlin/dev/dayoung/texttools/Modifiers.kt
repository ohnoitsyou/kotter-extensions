package dev.dayoung.texttools

typealias TransformerHandler = (next: String) -> String

fun interface Transformer : (TransformerHandler) -> TransformerHandler {
    companion object
}

object Transformers {
    object NoOp {
        operator fun invoke(): Transformer = Transformer { next -> { next(it) } }
    }

    object Uppercase {
        operator fun invoke(): Transformer = Transformer { next ->
            { next(it.uppercase()) }
        }
    }

    object Lowercase {
        operator fun invoke(): Transformer = Transformer { next ->
            { next(it.lowercase()) }
        }
    }

    object Kebabcase {
        operator fun invoke(): Transformer = Transformer { next ->
            { str ->
                val replacer = ReplaceSpace("-").invoke { it }
                next(replacer(str))
            }
        }
    }

    object ReplaceSpace {
        operator fun invoke(replacement: String): Transformer = Transformer { next ->
            { next(it.replace(" ", replacement)) }
        }
    }
}

val Transformer.Companion.NoOp: Transformer get() = Transformer { it }
fun Transformer.then(next: Transformer): Transformer = Transformer { this(next(it)) }
fun Transformer.then(next: TransformerHandler): TransformerHandler = this(next)
fun Transformer.get(str: String) = this { it }(str)

typealias InputValidatorHandler = (input: String?) -> String?

fun interface InputValidator : (InputValidatorHandler) -> InputValidatorHandler {
    companion object
}

val InputValidator.Companion.NoOp: InputValidator get() = InputValidator { it }
fun InputValidator.then(next: InputValidator): InputValidator = InputValidator { this(next(it)) }
fun InputValidator.then(next: InputValidatorHandler): InputValidatorHandler = this(next)
fun InputValidator.get(str: String) = this { it }(str)

object InputValidators {
    object NotEmptyValidator {
        operator fun invoke(): InputValidator = InputValidator { next ->
            { next(if (it?.isNotEmpty() == true) it else null) }
        }
    }

    object NotBlankValidator {
        operator fun invoke(): InputValidator = InputValidator { next ->
            { next(if (it?.isNotBlank() == true) it else null) }
        }
    }

    object NoOpValidator {
        operator fun invoke(): InputValidator = InputValidator { next ->
            { next(it) }
        }
    }

    object ValueInList {
        operator fun invoke(list: List<String>): InputValidator = InputValidator { next ->
            { next(if (it in list) it else null) }
        }
    }
}
