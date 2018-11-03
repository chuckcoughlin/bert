# Copyright (C) 2006-2018 Istituto Italiano di Tecnologia (IIT)
# All rights reserved.
#
# This software may be modified and distributed under the terms of the
# BSD-3-Clause license. See the accompanying LICENSE file for details.

function(yarp_colorize_string _out_var _color _bold _string)
  if(${ARGC} GREATER 4)
    set(_alt "${ARGN}")
  else()
    set(_alt "${_string}")
  endif()

  if($ENV{CLICOLOR_FORCE})
    unset(_bold_arg)
    if(_bold)
      set(_bold_arg "--bold")
    endif()
    execute_process(COMMAND ${CMAKE_COMMAND} -E cmake_echo_color --no-newline --${_color} ${_bold_arg} "${_string}"
                    OUTPUT_VARIABLE ${_out_var})
  else()
    set(${_out_var} "${_alt}")
  endif()

  set(${_out_var} ${${_out_var}} PARENT_SCOPE)
endfunction()

function(yarp_checkbox _var _out_var)
  if(${_var})
    yarp_colorize_string(_on green 1 "✔" "x")
    set(${_out_var} "[${_on}]")
  else()
    yarp_colorize_string(_off red 1 "✘" " ")
    set(${_out_var} "[${_off}]")
  endif()

  set(${_out_var} ${${_out_var}} PARENT_SCOPE)
endfunction()


function(yarp_print_with_checkbox _var _doc)
  yarp_checkbox(${_var} _check)
  message(STATUS " ${_check} ${_doc}")
endfunction()


function(yarp_print_feature _var _lev _doc)
  set(_indent "")
  foreach(i RANGE 0 ${_lev} 1)
    if(NOT ${i} EQUAL ${_lev})
      set(_indent "${_indent}  ")
    endif()
  endforeach()
  yarp_colorize_string(_help black 1 "${_var}")
  yarp_print_with_checkbox(${_var} "${_indent}${_doc} (${_help})")
endfunction()
