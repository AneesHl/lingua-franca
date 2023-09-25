package org.lflang.generator.cpp

import org.lflang.generator.PrependOperator
import org.lflang.joinWithLn
import org.lflang.toUnixString
import java.nio.file.Path

/** A C++ code generator for creating the required files for defining a ROS2 package. */
class CppRos2PackageGenerator(generator: CppGenerator, private val nodeName: String) {
    private val fileConfig = generator.fileConfig
    private val targetConfig = generator.targetConfig
    val reactorCppSuffix = targetConfig.runtimeVersion ?: "default"
    val reactorCppName = "reactor-cpp-$reactorCppSuffix"
    private val dependencies =
        listOf("rclcpp", "rclcpp_components", reactorCppName) + (targetConfig.ros2Dependencies ?: listOf<String>())

    @Suppress("PrivatePropertyName") // allows us to use capital S as variable name below
    private val S = '$' // a little trick to escape the dollar sign with $S

    fun generatePackageXml(): String {
        return """
            |<?xml version="1.0"?>
            |<?xml-model href="http://download.ros.org/schema/package_format3.xsd" schematypens="http://www.w3.org/2001/XMLSchema"?>
            |<package format="3">
            |  <name>${fileConfig.name}</name>
            |  <version>0.0.0</version>
            |  <description>Autogenerated from ${fileConfig.srcFile}</description>
            |  <maintainer email="todo@todo.com">Todo</maintainer>
            |  <license>Todo</license>
            |
            |  <buildtool_depend>ament_cmake</buildtool_depend>
            |  <buildtool_depend>ament_cmake_auto</buildtool_depend>
            |  
        ${" |"..dependencies.joinWithLn { "<depend>$it</depend>" }}
            |
            |  <test_depend>ament_lint_auto</test_depend>
            |  <test_depend>ament_lint_common</test_depend>
            |
            |  <exec_depend>ament_index_python</exec_depend>
            |
            |  <export>
            |    <build_type>ament_cmake</build_type>
            |  </export>
            |</package>
        """.trimMargin()
    }

    fun generatePackageCmake(sources: List<Path>): String {
        // Resolve path to the cmake include files if any was provided
        val includeFiles = targetConfig.cmakeIncludes.get()?.map { fileConfig.srcPath.resolve(it).toUnixString() }

        return with(PrependOperator) {
            with(CppGenerator) {
                """
                |cmake_minimum_required(VERSION $MINIMUM_CMAKE_VERSION)
                |project(${fileConfig.name} VERSION 0.0.0 LANGUAGES CXX)
                |
                |# require C++ $CPP_VERSION
                |set(CMAKE_CXX_STANDARD $CPP_VERSION CACHE STRING "The C++ standard is cached for visibility in external tools." FORCE)
                |set(CMAKE_CXX_STANDARD_REQUIRED ON)
                |set(CMAKE_CXX_EXTENSIONS OFF)
                |
                |set(DEFAULT_BUILD_TYPE "${targetConfig.buildType}")
                |if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
                |set    (CMAKE_BUILD_TYPE "$S{DEFAULT_BUILD_TYPE}" CACHE STRING "Choose the type of build." FORCE)
                |endif()
                |
                |# Invoke find_package() for all build and buildtool dependencies.
                |find_package(ament_cmake_auto REQUIRED)
                |ament_auto_find_build_dependencies()
                |
                |set(LF_MAIN_TARGET ${fileConfig.name})
                |
                |ament_auto_add_library($S{LF_MAIN_TARGET} SHARED
                |    src/$nodeName.cc
            ${" |    "..sources.joinWithLn { "src/$it" }}
                |)
                |ament_target_dependencies($S{LF_MAIN_TARGET} ${dependencies.joinToString(" ")})
                |target_include_directories($S{LF_MAIN_TARGET} PUBLIC
                |    "$S{LF_SRC_PKG_PATH}/src"
                |    "$S{PROJECT_SOURCE_DIR}/src/"
                |    "$S{PROJECT_SOURCE_DIR}/src/__include__"
                |)
                |target_link_libraries($S{LF_MAIN_TARGET} $reactorCppName)
                |
                |rclcpp_components_register_node($S{LF_MAIN_TARGET}
                |  PLUGIN "$nodeName"
                |  EXECUTABLE $S{LF_MAIN_TARGET}_exe
                |)
                |
                |if(MSVC)
                |  target_compile_options($S{LF_MAIN_TARGET} PRIVATE /W4)
                |else()
                |  target_compile_options($S{LF_MAIN_TARGET} PRIVATE -Wall -Wextra -pedantic)
                |endif()
                |
                |ament_auto_package()
                |
            ${" |"..(includeFiles?.joinWithLn { "include(\"$it\")" } ?: "")}
            """.trimMargin()
            }
        }
    }

    fun generateBinScript(): String {
        val relPath = fileConfig.binPath.relativize(fileConfig.outPath).toUnixString()

        return """
            |#!/bin/bash
            |script_dir="$S(dirname -- "$S(readlink -f -- "${S}0")")"
            |source "$S{script_dir}/$relPath/install/setup.sh"
            |ros2 run ${fileConfig.name} ${fileConfig.name}_exe
        """.trimMargin()
    }
}
