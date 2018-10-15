# Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
# All rights reserved.
#
# This software may be modified and distributed under the terms of the
# BSD-3-Clause license. See the accompanying LICENSE file for details.

include(InstallBasicPackageFiles)

macro(YARP_INSTALL_BASIC_PACKAGE_FILES _export)
  set(_options )
  set(_oneValueArgs FIRST_TARGET
                    STATIC_CONFIG_TEMPLATE)
  set(_multiValueArgs DEPENDENCIES
                      PRIVATE_DEPENDENCIES)
  cmake_parse_arguments(_YIBPF "${_options}" "${_oneValueArgs}" "${_multiValueArgs}" ${ARGN})

  unset(_deps)
  foreach(_dep ${_YIBPF_DEPENDENCIES})
    if("${_dep}" MATCHES "^YARP_")
      list(APPEND _deps "${_dep} HINTS \"\${CMAKE_CURRENT_LIST_DIR}/..\" NO_DEFAULT_PATH")
    else()
      # Add either the private libraries built by YARP, or the ones on the
      # system
      string(TOUPPER "${_dep}" _DEP)
      string(REGEX REPLACE " +" "" _DEP ${_DEP})
      if(YARP_BUILD_${_DEP})
        list(APPEND _deps "YARP_priv_${_dep}")
      else()
        list(APPEND _deps "${_dep}")
      endif()
    endif()
  endforeach()

  unset(_priv_deps)
  foreach(_dep ${_YIBPF_PRIVATE_DEPENDENCIES})
    if("${_dep}" MATCHES "^YARP_")
      list(APPEND _priv_deps "${_dep} HINTS \"\${CMAKE_CURRENT_LIST_DIR}/..\" NO_DEFAULT_PATH")
    else()
      # Add either the private libraries built by YARP, or the ones on the
      # system
      string(TOUPPER "${_dep}" _DEP)
      string(REGEX REPLACE " +" "" _DEP ${_DEP})
      if(YARP_BUILD_${_DEP})
        list(APPEND _deps "YARP_priv_${_dep} HINTS \"\${CMAKE_CURRENT_LIST_DIR}/..\" NO_DEFAULT_PATH")
      else()
        list(APPEND _priv_deps "${_dep}")
      endif()
    endif()
  endforeach()

  unset(_first_target)
  if(DEFINED _YIBPF_FIRST_TARGET)
    set(_first_target FIRST_TARGET ${_YIBPF_FIRST_TARGET})
  endif()

  unset(_config_template)
  if(NOT BUILD_SHARED_LIBS AND DEFINED _YIBPF_STATIC_CONFIG_TEMPLATE)
    set(_config_template CONFIG_TEMPLATE "${_YIBPF_STATIC_CONFIG_TEMPLATE}")
  endif()
  install_basic_package_files(${_export}
                              ${_first_target}
                              ${_config_template}
                              VERSION ${YARP_VERSION}
                              COMPATIBILITY SameMajorVersion
                              EXPORT_DESTINATION ${_export}
                              DEPENDENCIES ${_deps}
                              PRIVATE_DEPENDENCIES ${_priv_deps}
                              NO_CHECK_REQUIRED_COMPONENTS_MACRO
                              NO_SET_AND_CHECK_MACRO
                              NO_COMPATIBILITY_VARS
                              NAMESPACE YARP::
                              UPPERCASE_FILENAMES
                              ${_YIBPF_UNPARSED_ARGUMENTS})
endmacro()
