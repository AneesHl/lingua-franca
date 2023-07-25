package org.lflang.generator.ts

import org.lflang.*
import org.lflang.generator.PrependOperator
import org.lflang.lf.*
import org.lflang.validation.AttributeSpec
import java.util.*

/**
 * Reactor generator for TypeScript target.
 *
 *  @author Matt Weber
 *  @author Edward A. Lee
 *  @author Marten Lohstroh
 *  @author Christian Menard
 *  @author Hokeun Kim
 */
class TSReactorGenerator(
    private val tsGenerator: TSGenerator,
    private val messageReporter: MessageReporter,
    private val targetConfig: TargetConfig
) {

    companion object {
        const val MIN_OUTPUT_DELAY_STATEMENT =
            """
                if (defaultFederateConfig.minOutputDelay !== undefined) {
                    __app.setMinDelayFromPhysicalActionToFederateOutput(defaultFederateConfig.minOutputDelay);
                }
            """
    }

    /** Generate the main app instance. This function is only used once
     *  because all other reactors are instantiated as properties of the
     *  main one.
     *  @param instance A reactor instance.
     */
    private fun generateMainReactorInstance(
        defn: Instantiation,
        mainParameters: Set<Parameter>
    ): String {

        val fullName = defn.name

        // Iterate through parameters in the order they appear in the
        // main reactor class. If the parameter is typed such that it can
        // be a custom command line argument, use the parameter's command line
        // assignment variable ("__CL" + the parameter's name). That variable will
        // be undefined if the command line argument wasn't specified. Otherwise
        // use undefined in the constructor.
        val mainReactorParams = defn.reactorClass.toDefinition().parameters.joinWithCommas { p ->
            if (p in mainParameters) "__CL" + p.name
            else "undefined"
        }

        return """
        |// ************* Instance $fullName of class ${defn.reactorClass.name}
        |let __app;
        |if (!__noStart) {
        |    __app = new $fullName(__timeout, __keepAlive, __fast, __federationID, $mainReactorParams () => true, () => process.exit(1));
        |}
        """.trimMargin()
    }

    /** Generate code to call the _start function on the main App
     *  instance to start the runtime
     *  @param instance A reactor instance.
     */
    private fun generateRuntimeStart(defn: Instantiation): String {
        val isFederate = AttributeUtils.isFederate(defn.reactor)
        return with(PrependOperator) {
                """
            |// ************* Starting Runtime for ${defn.name} + of class ${defn.reactorClass.name}.
            |if (!__noStart && __app) {
${"         |"..MIN_OUTPUT_DELAY_STATEMENT.takeIf { isFederate }.orEmpty()}
            |    __app._start();
            |}
            |
            """
        }.trimMargin()
    }

    private fun generateReactorPreambles(preambles: List<Preamble>): String =
        preambles.joinToString("\n") { preamble ->
            with(PrependOperator) {
                """
            |// *********** From the preamble, verbatim:
${"             |"..preamble.code.toText()}
            |// *********** End of preamble."""
            }.trimMargin()
        }

    // private fun getNetworkMessageActions(reactor: Reactor): MutableList<String> {
    //     val attribute = AttributeUtils.findAttributeByName(reactor, "_fed_config")
    //     val actionsStr = AttributeUtils.getAttributeParameter(attribute, AttributeSpec.NETWORK_MESSAGE_ACTIONS)
    //     var actionsList = actionsStr?.split(",")?.filter { it.isNotEmpty()} ?: emptyList()
    //     actionsList = actionsList.toMutableList()

    //     val childReactors = reactor.instantiations
    //     var actionsListCount = 0
    //     for (childReactor in childReactors) {
    //         if (childReactor.reactorClass.name.take(15) == "NetworkReceiver") {
    //             // FIXME: Assume that the order of childReactor and attribute list are identical.
    //             // This assumption might bring some erros
    //             actionsList[actionsListCount] = childReactor.name + "." + actionsList[actionsListCount]
    //             actionsListCount++
    //         }
    //     }
    //     return actionsList
    // }

    fun generateReactor(reactor: Reactor): String {
        var reactorName = reactor.name
        if (!reactor.typeParms.isEmpty()) {
            reactorName +=
                reactor.typeParms.joinToString(", ", "<", ">") { it.toText() }
        }

        val isFederate = AttributeUtils.isFederate(reactor)
        val isNetworkReactor = reactorName.take(7) == "Network"
        // val networkMessageActions = getNetworkMessageActions(reactor)

        // NOTE: type parameters that are referenced in ports or actions must extend
        // Present in order for the program to type check.
        val classDefinition: String = if (reactor.isMain) {
            if (isFederate) {
                "class $reactorName extends __FederatedApp {"
            } else {
                "class $reactorName extends __App {"
            }
        } else {
            if (isNetworkReactor) {
                val networkInputType = if (reactor.actions.size == 0) "unknown" else reactor.actions[0].tsActionType
                "export class $reactorName extends __NetworkReactor<$networkInputType> {"    
            } else {
                "export class $reactorName extends __Reactor {"
            }
        }

        val instanceGenerator = TSInstanceGenerator(reactor)
        val timerGenerator = TSTimerGenerator(reactor.timers)
        val parameterGenerator = TSParameterGenerator(reactor.parameters)
        val stateGenerator = TSStateGenerator(reactor.stateVars)
        val actionGenerator = TSActionGenerator(reactor.actions)
        val portGenerator = TSPortGenerator(reactor.inputs, reactor.outputs)

        val constructorGenerator = TSConstructorGenerator(messageReporter, reactor)
        return with(PrependOperator) {
            """
                |// =============== START reactor class ${reactor.name}
                |${generateReactorPreambles(reactor.preambles)}
                |
                |$classDefinition
            ${" |    "..instanceGenerator.generateClassProperties()}
            ${" |    "..timerGenerator.generateClassProperties()}
            ${" |    "..parameterGenerator.generateClassProperties()}
            ${" |    "..stateGenerator.generateClassProperties()}
            ${" |    "..actionGenerator.generateClassProperties()}
            ${" |    "..portGenerator.generateClassProperties()}
            ${" |    "..constructorGenerator.generateConstructor(targetConfig, instanceGenerator, timerGenerator, parameterGenerator,
                stateGenerator, actionGenerator, portGenerator, isFederate)}
                |}
                |// =============== END reactor class ${reactor.name}
                |
            """.trimMargin()
        }
    }

    fun generateMainReactorInstanceAndStart(
        mainDef: Instantiation,
        mainParameters: Set<Parameter>
    ): String {
        return with(PrependOperator) {
            """
${"         |"..generateMainReactorInstance(mainDef, mainParameters)}
${"         |"..generateRuntimeStart(mainDef)}
            |
            """
        }.trimMargin()
    }
}
