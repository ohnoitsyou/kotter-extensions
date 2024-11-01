package dev.dayoung.kotterextensions

import com.varabyte.kotter.foundation.input.Completions
import com.varabyte.kotter.foundation.input.Keys
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.multilineInput
import com.varabyte.kotter.foundation.input.onInputChanged
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.onKeyPressed
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.text.black
import com.varabyte.kotter.foundation.text.p
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.foundation.text.textLine
import com.varabyte.kotter.foundation.text.white
import com.varabyte.kotter.runtime.Session
import dev.dayoung.texttools.InputValidator
import dev.dayoung.texttools.NoOp
import dev.dayoung.texttools.Transformer
import dev.dayoung.texttools.get
import java.util.Optional

fun Session.multiKeyValuePrompt(): Map<String, String> {
    val kvList = mutableMapOf<String, String>()
    var newKv: Optional<Pair<String, String>>
    do {
        newKv = keyValuePrompt()
        newKv.ifPresent {
            kvList[it.first] = it.second
        }
    } while (newKv.isPresent)
    return kvList
}

fun Session.keyValuePrompt(): Optional<Pair<String, String>> {
    val attrKey = prompt("Key: ")
    if (attrKey.isNotEmpty()) {
        val attrValue = prompt("Value: ")
        if (attrValue.isNotEmpty()) {
            return Optional.of(attrKey to attrValue)
        }
    }
    return Optional.empty()
}

fun Session.promptMultiline(prompt: String): String {
    var retVal = ""
    section {
        textLine(prompt)
        multilineInput()
    }.runUntilInputEntered {
        onInputEntered {
            if (input.isNotBlank()) {
                retVal = input.lowercase()
            } else {
                rejectInput()
            }
        }
    }
    return retVal
}

fun Session.prompt(
    prompt: String,
    completions: Completions? = null,
    inputModifier: Transformer = Transformer.NoOp,
    inputValidator: InputValidator = InputValidator.NoOp,
): String {
    var retVal = ""
    section {
        text(prompt)
        input(completions)
    }.runUntilInputEntered {
        onInputChanged {
            input = inputModifier.get(input)
        }
        onInputEntered {
            if (inputValidator.get(input) != null) {
                retVal = input
            } else {
                rejectInput()
            }
        }
    }
    return retVal
}

fun Session.confirmation(
    prompt: String = "Confirm: ",
    completions: List<String> = listOf("Yes", "No"),
    affirmative: String = completions.first(),
): Boolean {
    var confirmation by liveVarOf(false)
    val c = Completions(*completions.toTypedArray())
    section {
        text("$prompt (${completions.joinToString()}) ")
        input(c)
    }.runUntilInputEntered {
        onInputEntered {
            // Does completing the current input equal the affirmative option
            confirmation = "$input${c.complete(input) ?: ""}".lowercase() == affirmative.lowercase()
        }
    }
    return confirmation
}

fun <T> Session.multilineSelect(prompt: String, options: List<Pair<T, String>>): Optional<Pair<T, String>> {
    var highlightOption by liveVarOf(0)
    section {
        p {
            textLine(prompt)
            options.forEachIndexed { idx, option ->
                if (idx == highlightOption) {
                    white(isBright = true) {
                        textLine("> ${option.second}")
                    }
                } else {
                    black(isBright = true) {
                        textLine("  ${option.second}")
                    }
                }
            }
        }
    }.runUntilSignal {
        onKeyPressed {
            when (key) {
                Keys.UP -> highlightOption = (--highlightOption).coerceIn(0..options.lastIndex)
                Keys.DOWN -> highlightOption = (++highlightOption).coerceIn(0..options.lastIndex)
                Keys.PAGE_UP -> highlightOption = (5 - highlightOption).coerceIn(0..options.lastIndex)
                Keys.PAGE_DOWN -> highlightOption = (5 + highlightOption).coerceIn(0..options.lastIndex)
                Keys.HOME -> highlightOption = 0
                Keys.END -> highlightOption = options.lastIndex
                Keys.ESC -> {
                    highlightOption = -1; signal()
                }

                Keys.ENTER -> signal()
                else -> {}
            }
        }
    }
    return if (highlightOption >= 0) {
        Optional.of(options[highlightOption])
    } else {
        Optional.empty()
    }
}
